# 2. Category 도메인

작업 순서: 2번째
의존성: User (고정 데모 사용자 컨텍스트만 사용)
범위: 청소 카테고리 CRUD, 카테고리 프리셋 조회, 카테고리 상태(due/soon/good) 계산. 프리셋 초기 값 시딩은 이번 범위에서 제외한다(별도 SQL로 처리).

## 2.1 데이터 구조

### cleaning_categories

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 카테고리 ID |
| user_id | uuid fk users.id | 소유 사용자 |
| preset_key | varchar(50) nullable | 프리셋 키 |
| name | varchar(40) | 카테고리명 |
| icon | varchar(40) | 아이콘 키 |
| cycle_days | int | 관리 주기 |
| last_done_at | timestamptz | 마지막 완료 시각 |
| note | text | 설명 문구 |
| sort_order | int | 노출 순서 |
| is_active | boolean | 활성 여부 |
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

제약:

- `cycle_days > 0`
- 같은 사용자 안에서 활성 카테고리 이름 중복 방지
- 같은 사용자 안에서 활성 프리셋 중복 방지

인덱스: `idx_categories_user_active_sort(user_id, is_active, sort_order)`

### category_presets

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| key | varchar(50) pk | 프리셋 키 |
| name | varchar(40) | 이름 |
| icon | varchar(40) | 아이콘 키 |
| cycle_days | int | 기본 주기 |
| note | text | 기본 설명 |
| sort_order | int | 노출 순서 |
| is_default | boolean | 신규 사용자 기본 생성 여부 |
| is_active | boolean | 활성 여부 |

프리셋 행 데이터(욕실/주방 등 실제 값)는 별도 SQL로 시딩하므로 이번 작업 범위가 아니다. 테이블 구조와 조회 API만 구현한다.

## 2.2 도메인 로직

### 카테고리 상태 계산

입력: `last_done_at`, `cycle_days`, 사용자 타임존 기준 `today`

```text
next_due_at = last_done_at + cycle_days
days_until_next = ceil((next_due_at - now) / 1 day)
```

| 조건 | code | label |
| --- | --- | --- |
| `days_until_next <= 0` | `due` | `이번 주에 챙기면 좋아요` |
| `days_until_next <= 2` | `soon` | `{N}일 안에 하면 좋아요` |
| 그 외 | `good` | `{N}일 뒤 다시 보면 충분해요` |

상태와 `next_due_at`은 저장하지 않고 조회 시점에 계산한다.

## 2.3 API 명세

### GET /categories

활성 카테고리 목록 조회.

Response `200`:

```json
{
  "data": [
    {
      "id": "cat_kitchen",
      "name": "주방",
      "icon": "kitchen",
      "cycleDays": 7,
      "lastDoneAt": "2026-07-06T20:00:00+09:00",
      "nextDueAt": "2026-07-13T20:00:00+09:00",
      "note": "배수구와 조리대 표면을 기준으로 잡아요.",
      "status": {
        "code": "good",
        "label": "3일 뒤 다시 보면 충분해요",
        "daysUntilNext": 3
      },
      "createdAt": "2026-07-10T09:00:00+09:00",
      "updatedAt": "2026-07-10T09:00:00+09:00"
    }
  ]
}
```

정렬: `sort_order` 오름차순, `is_active=true`만 반환.

### GET /category-presets

Response `200`:

```json
{
  "data": [
    {
      "key": "pet",
      "name": "반려동물",
      "icon": "floor",
      "cycleDays": 7,
      "note": "털, 냄새, 패드 주변을 한 카테고리로 관리해요."
    }
  ]
}
```

### POST /categories

프리셋 기반 또는 직접 입력 기반 생성.

Request (프리셋):

```json
{ "presetKey": "pet" }
```

Request (직접 입력):

```json
{
  "name": "현관",
  "icon": "floor",
  "cycleDays": 7,
  "note": "신발장과 현관 먼지를 함께 관리해요."
}
```

Response `201`: 생성된 카테고리 (상태 포함, 위 목록 응답의 item과 동일 형태)

에러:

| 코드 | 조건 |
| --- | --- |
| `CATEGORY_DUPLICATED` | 같은 프리셋 또는 같은 이름의 활성 카테고리가 이미 있음 |
| `CATEGORY_PRESET_NOT_FOUND` | 존재하지 않는 프리셋 |

### PATCH /categories/{categoryId}

Request:

```json
{
  "cycleDays": 14,
  "name": "욕실",
  "note": "물때와 습기를 기준으로 관리해요."
}
```

Response `200`: 갱신된 카테고리 (상태 재계산 포함)

검증:

- `cycleDays`는 `3`, `7`, `14`, `21`, `28` 중 하나여야 한다.
- 소유자(고정 데모 사용자) 소유 카테고리만 수정 가능.

## 2.4 완료 기준

1. 활성 카테고리 목록이 `sort_order` 순으로 반환되고 각 항목에 계산된 상태가 포함된다.
2. 프리셋 목록 조회가 가능하다.
3. 프리셋 기반/직접 입력 기반 카테고리 생성이 가능하고 중복이 방지된다.
4. 주기·이름·설명 수정 후 상태가 재계산되어 응답된다.
