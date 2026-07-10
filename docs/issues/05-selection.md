# 5. Selection 도메인

작업 순서: 5번째
의존성: User
범위: 브랜드 셀렉션(추천 상품/서비스) 조회, 저장/해제, 외부 보기 클릭 기록. 셀렉션 원본 데이터(상품 목록 등)는 별도 SQL로 시딩하므로 이번 범위에서 제외한다.

## 5.1 데이터 구조

### selection_items

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 셀렉션 ID |
| slug | varchar(80) unique | 외부 노출 키 |
| type | varchar(30) | `kit`, `product`, `service`, `subscription` |
| category | varchar(40) | `전체`, `욕실` 등 |
| title | varchar(120) | 제목 |
| label | varchar(40) | 라벨 |
| price_text | varchar(80) | 가격대 문구 |
| affiliate_text | varchar(40) | 제휴 여부 문구 |
| reason | text | 고른 이유 |
| fit_for | text | 적합한 상황 |
| notice | text | 고지 문구 |
| is_highlighted | boolean | 추천 강조 여부 |
| external_url | text nullable | 외부 보기 URL |
| status | varchar(20) | `draft`, `published`, `hidden` |
| sort_order | int | 노출 순서 |

인덱스: `idx_selection_items_status_category(status, category, is_highlighted, sort_order)`

### provider_options

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 제공업체 옵션 ID |
| selection_item_id | uuid fk selection_items.id | 셀렉션 |
| name | varchar(80) | 업체명 |
| rating_text | varchar(20) nullable | 평점 문구 |
| price_text | varchar(80) | 가격 문구 |
| note | varchar(120) | 제공 방식 또는 특징 |
| external_url | text nullable | 외부 URL |
| sort_order | int | 노출 순서 |

### saved_selections

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 저장 ID |
| user_id | uuid fk users.id | 사용자 |
| selection_item_id | uuid fk selection_items.id | 셀렉션 |
| created_at | timestamptz | 저장 시각 |

제약: `unique(user_id, selection_item_id)`

## 5.2 운영 정책 (구현 시 반영)

- 가격은 고정 금액 보장처럼 표현하지 않고 `가격대`, `예시`, `범위`로 관리한다.
- 외부 URL이 있는 경우에도 최종 조건 확인 고지(`notice`)를 함께 노출한다.
- `status=hidden` 또는 `draft` 항목은 사용자 API에서 제외한다(`status=published`만 노출).

## 5.3 API 명세

### GET /selections

Query: `category`(선택), `cursor`, `limit`

정렬: `is_highlighted desc, sort_order asc, created_at desc`

필터 규칙: `category=전체` 요청 시 전체 공개 항목 모두 반환. 특정 카테고리 요청 시 `category in ('전체', 요청 카테고리)` 반환.

Response `200`:

```json
{
  "data": [
    {
      "id": "starter-kit",
      "type": "kit",
      "category": "전체",
      "title": "자취 첫 달 기본 청소 키트",
      "label": "가장 먼저 담기 좋음",
      "priceText": "19,000원대",
      "affiliateText": "일부 제휴",
      "reason": "세제 종류를 늘리기보다 매주 쓰는 것만 담았어요.",
      "fitFor": "처음 자취를 시작했거나 청소 도구가 거의 없는 사용자",
      "isSaved": true,
      "providers": []
    }
  ],
  "meta": { "nextCursor": null }
}
```

`isSaved`는 `saved_selections`를 left join해 계산한다.

### GET /selections/{selectionId}

Response `200`: `providers` 배열(연결된 `provider_options`)을 포함한 상세 정보. 필드는 목록 응답과 동일하되 `notice`, `providers` 추가.

### PUT /selections/{selectionId}/save

```text
insert into saved_selections(user_id, selection_item_id)
on conflict do nothing
```

Response `200`:

```json
{ "data": { "selectionId": "starter-kit", "isSaved": true, "savedAt": "2026-07-10T12:00:00+09:00" } }
```

멱등 처리(같은 요청 반복해도 동일 결과).

### DELETE /selections/{selectionId}/save

```text
delete from saved_selections
where user_id = :userId and selection_item_id = :selectionItemId
```

Response `204`.

### GET /me/saved-selections

사용자가 저장한 셀렉션을 저장 시각 최신순으로 반환.

### POST /selections/{selectionId}/external-view

Request:

```json
{ "providerId": "provider_01" }
```

Response `200`:

```json
{
  "data": {
    "selectionId": "trash-pickup",
    "providerId": "provider_01",
    "externalUrl": "https://example.com",
    "notice": "외부 페이지에서 최종 가격과 조건을 확인하세요."
  }
}
```

`externalUrl`이 준비되지 않은 경우 빈 값으로 두고 클릭 이벤트만 기록해도 된다. 앱 내부에서 결제/예약 확정은 처리하지 않는다.

## 5.4 완료 기준

1. 카테고리 필터가 적용된 셀렉션 목록이 정렬 규칙대로 반환된다.
2. 상세 조회 시 제공업체 옵션이 함께 반환된다.
3. 저장/해제가 멱등적으로 동작하고 목록·상세·마이 화면에 일관되게 반영된다.
4. 외부 보기 클릭이 기록된다.
