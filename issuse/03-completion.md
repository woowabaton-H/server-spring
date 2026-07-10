# 3. Completion 도메인

작업 순서: 3번째
의존성: Category (완료 대상 카테고리가 존재해야 함)
범위: 카테고리 완료 처리 트랜잭션, 완료 기록 조회.

## 3.1 데이터 구조

### completion_logs

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 완료 기록 ID |
| user_id | uuid fk users.id | 사용자 |
| category_id | uuid nullable | 카테고리 ID. 삭제된 카테고리 기록 보존을 위해 nullable |
| category_name | varchar(40) | 완료 당시 카테고리명 스냅샷 |
| completed_at | timestamptz | 완료 시각 |
| created_at | timestamptz | 기록 생성 시각 |

인덱스:

- `idx_completion_logs_user_completed(user_id, completed_at desc)`
- `idx_completion_logs_category(category_id, completed_at desc)`

## 3.2 도메인 로직

### 완료 처리 트랜잭션

`POST /categories/{id}/complete` 처리:

```text
begin
  category = select category for update
  validate ownership and is_active
  insert completion_logs
  update cleaning_categories.last_done_at
commit
return updated category + log
```

트랜잭션이 필요한 이유:

- 완료 기록만 생성되고 카테고리 날짜가 갱신되지 않는 상태를 막는다.
- 연속 클릭 시 레이스 컨디션을 줄인다.

중복 완료 정책: 같은 날 같은 카테고리의 여러 완료 기록을 허용한다(별도 유니크 제약 없음).

## 3.3 API 명세

### POST /categories/{categoryId}/complete

Request:

```json
{ "completedAt": "2026-07-10T12:00:00+09:00" }
```

Response `201`:

```json
{
  "data": {
    "category": {
      "id": "cat_bath",
      "name": "욕실",
      "cycleDays": 14,
      "lastDoneAt": "2026-07-10T12:00:00+09:00",
      "nextDueAt": "2026-07-24T12:00:00+09:00",
      "status": {
        "code": "good",
        "label": "14일 뒤 다시 보면 충분해요",
        "daysUntilNext": 14
      }
    },
    "log": {
      "id": "log_10",
      "categoryId": "cat_bath",
      "categoryName": "욕실",
      "completedAt": "2026-07-10T12:00:00+09:00"
    },
    "toastMessage": "욕실 완료. 다음 관리는 7월 24일에 보면 충분해요."
  }
}
```

### GET /completion-logs

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| limit | 아니오 | 기본 20, 최대 100 |
| cursor | 아니오 | 다음 페이지 커서 |
| from | 아니오 | 조회 시작일 |
| to | 아니오 | 조회 종료일 |

Response `200`:

```json
{
  "data": [
    {
      "id": "log_10",
      "categoryId": "cat_bath",
      "categoryName": "욕실",
      "completedAt": "2026-07-10T12:00:00+09:00"
    }
  ],
  "meta": { "nextCursor": null }
}
```

## 3.4 완료 기준

1. 완료 처리 시 `completion_logs` 생성과 `cleaning_categories.last_done_at` 갱신이 하나의 트랜잭션으로 처리된다.
2. 응답에 갱신된 카테고리(상태 재계산 포함)와 생성된 로그가 함께 반환된다.
3. 완료 기록을 최신순으로 커서 페이지네이션 조회할 수 있다.
