# 4. Home / Me 요약 도메인

작업 순서: 4번째
의존성: User, Category, Completion (셋 모두 구현되어 있어야 함)
범위: 별도 테이블 없이 기존 테이블(users, cleaning_categories, completion_logs)을 조합한 집계 API 두 개.

## 4.1 API 명세

### GET /home

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| today | 아니오 | 상태 계산 기준일. 없으면 서버가 사용자 타임존 기준 오늘을 사용 |

Response `200`:

```json
{
  "data": {
    "today": "2026-07-10",
    "message": "오늘 청소부터 챙겨요.",
    "monthlyCompletionCount": 5,
    "categories": [
      {
        "id": "cat_bath",
        "name": "욕실",
        "icon": "bath",
        "cycleDays": 14,
        "lastDoneAt": "2026-07-01T09:00:00+09:00",
        "nextDueAt": "2026-07-15T09:00:00+09:00",
        "note": "물때와 습기만 잡아도 관리가 쉬워져요.",
        "status": {
          "code": "good",
          "label": "5일 뒤 다시 보면 충분해요",
          "daysUntilNext": 5
        }
      }
    ],
    "recentLogs": [
      {
        "id": "log_01",
        "categoryId": "cat_trash",
        "categoryName": "쓰레기/수거",
        "completedAt": "2026-07-08T21:00:00+09:00"
      }
    ]
  }
}
```

처리 규칙:

- `categories`는 활성 카테고리만, Category 도메인의 상태 계산 로직을 그대로 재사용한다.
- `monthlyCompletionCount`는 사용자 타임존 기준 이번 달 `completion_logs` 수.
- `recentLogs`는 `completed_at` 최신순 일부(예: 최근 5건).
- `unreadNotificationCount` 필드는 Notification 도메인이 없으므로 이번 범위에서 생략한다(프론트 계약 테스트 대상에서 제외).

### GET /me/summary

Response `200`:

```json
{
  "data": {
    "profile": {
      "id": "usr_01",
      "name": "보송",
      "avatarText": "보"
    },
    "stats": {
      "monthlyCompletionCount": 5,
      "categoryCount": 6,
      "savedSelectionCount": 1
    },
    "weeklyFootprints": [
      { "weekStartDate": "2026-04-20", "completionCount": 1, "level": 1 },
      { "weekStartDate": "2026-07-06", "completionCount": 2, "level": 2 }
    ],
    "recentLogs": [
      {
        "id": "log_10",
        "categoryId": "cat_bath",
        "categoryName": "욕실",
        "completedAt": "2026-07-10T12:00:00+09:00"
      }
    ],
    "savedSelections": []
  }
}
```

처리 규칙:

- `weeklyFootprints`는 사용자 타임존 기준 주 시작일로 최근 12주를 집계하고, 데이터가 없는 주도 빈 값(0건)으로 포함한다.
- `level`은 주간 완료 수를 0~3 단계로 변환한 값이다. 제안 기준: 0건 → 0, 1건 → 1, 2~3건 → 2, 4건 이상 → 3. (임의 조정 가능)
- `savedSelectionCount`, `savedSelections`는 Selection 도메인이 없는 시점에는 빈 값(0, [])으로 응답해도 무방하며, Selection 도메인 구현 후 연결한다.
- 모든 집계는 실시간 계산으로 처리한다(별도 집계 테이블/배치 불필요).

## 4.2 완료 기준

1. `GET /home`이 카테고리 상태, 이번 달 완료 수, 최근 완료 기록을 함께 반환한다.
2. `GET /me/summary`가 프로필, 통계, 최근 12주 주간 완료 집계, 최근 기록을 반환한다.
3. 두 API 모두 저장된 집계 테이블 없이 요청 시점 계산으로 동작한다.
