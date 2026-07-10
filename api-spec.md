# 클린루프 API 문서

작성일: 2026-07-10  
기준 문서: `final/docs/functional-requirements.md`  
범위: MVP API 설계. 직접 판매, 결제, 외부 예약 확정 API는 제외한다.

## 1. API 개요

클린루프 API는 모바일 또는 웹 클라이언트가 청소 카테고리 주기, 완료 기록, 셀렉션, 커뮤니티, 마이 기록, 알림을 조회하고 갱신하기 위한 JSON REST API다.

Base URL:

```text
/api/v1
```

공통 규칙:

| 항목 | 규칙 |
| --- | --- |
| Content-Type | `application/json; charset=utf-8` |
| 인증 | `Authorization: Bearer <accessToken>` |
| 시간 | ISO-8601 문자열. 예: `2026-07-10T12:00:00+09:00` |
| 기본 타임존 | `Asia/Seoul` |
| ID | UUID 문자열 |
| 페이지네이션 | `limit`, `cursor` 기반 |
| 삭제 | MVP에서는 물리 삭제보다 `isActive`, `status` 갱신을 우선 |

## 2. 응답 형식

### 2.1 성공 응답

```json
{
  "data": {},
  "meta": {
    "requestId": "req_01j..."
  }
}
```

목록 응답:

```json
{
  "data": [],
  "meta": {
    "nextCursor": "eyJpZCI6...",
    "requestId": "req_01j..."
  }
}
```

### 2.2 에러 응답

```json
{
  "error": {
    "code": "CATEGORY_NOT_FOUND",
    "message": "카테고리를 찾을 수 없습니다.",
    "details": {}
  },
  "meta": {
    "requestId": "req_01j..."
  }
}
```

공통 HTTP 상태:

| 상태 | 의미 |
| --- | --- |
| 200 | 조회 또는 갱신 성공 |
| 201 | 생성 성공 |
| 204 | 응답 본문 없는 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 또는 상태 충돌 |
| 422 | 검증 실패 |
| 429 | 요청 제한 |
| 500 | 서버 오류 |

## 3. 인증 / 사용자

최종 기능 요구서에는 복잡한 가입 흐름이 없으므로 MVP는 게스트 세션을 기본으로 둔다. 이후 정식 로그인으로 확장할 수 있다.

### 3.1 게스트 세션 생성

```http
POST /api/v1/auth/guest
```

Request:

```json
{
  "deviceId": "device_abc",
  "timezone": "Asia/Seoul"
}
```

Response `201`:

```json
{
  "data": {
    "accessToken": "token...",
    "user": {
      "id": "usr_01",
      "name": "김보송",
      "avatarText": "보",
      "timezone": "Asia/Seoul",
      "createdAt": "2026-07-10T09:00:00+09:00"
    }
  }
}
```

처리 규칙:

- 신규 게스트 사용자에게 기본 청소 카테고리를 생성한다.
- 같은 `deviceId`로 재요청하면 기존 사용자 세션을 반환할 수 있다.

### 3.2 내 정보 조회

```http
GET /api/v1/me
```

Response:

```json
{
  "data": {
    "id": "usr_01",
    "name": "김보송",
    "avatarText": "보",
    "timezone": "Asia/Seoul",
    "createdAt": "2026-07-10T09:00:00+09:00"
  }
}
```

### 3.3 내 정보 수정

```http
PATCH /api/v1/me
```

Request:

```json
{
  "name": "보송",
  "avatarText": "보"
}
```

Response:

```json
{
  "data": {
    "id": "usr_01",
    "name": "보송",
    "avatarText": "보",
    "timezone": "Asia/Seoul"
  }
}
```

## 4. 홈

### 4.1 홈 요약 조회

```http
GET /api/v1/home
```

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| today | 아니오 | 상태 계산 기준일. 없으면 서버가 사용자 타임존 기준 오늘을 사용 |

Response:

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
    ],
    "unreadNotificationCount": 1
  }
}
```

처리 규칙:

- `categories`는 활성 카테고리만 반환한다.
- `nextDueAt`과 `status`는 서버에서 계산해 반환한다.
- `monthlyCompletionCount`는 사용자 타임존 기준 현재 월의 완료 기록 수다.

## 5. 청소 카테고리

### 5.1 카테고리 목록 조회

```http
GET /api/v1/categories
```

Response:

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

### 5.2 추천 카테고리 프리셋 조회

```http
GET /api/v1/category-presets
```

Response:

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

### 5.3 카테고리 추가

```http
POST /api/v1/categories
```

Request:

```json
{
  "presetKey": "pet"
}
```

직접 입력 확장 요청:

```json
{
  "name": "현관",
  "icon": "floor",
  "cycleDays": 7,
  "note": "신발장과 현관 먼지를 함께 관리해요."
}
```

Response `201`:

```json
{
  "data": {
    "id": "cat_pet",
    "name": "반려동물",
    "icon": "floor",
    "cycleDays": 7,
    "lastDoneAt": "2026-07-10T12:00:00+09:00",
    "nextDueAt": "2026-07-17T12:00:00+09:00",
    "note": "털, 냄새, 패드 주변을 한 카테고리로 관리해요.",
    "status": {
      "code": "good",
      "label": "7일 뒤 다시 보면 충분해요",
      "daysUntilNext": 7
    }
  }
}
```

에러:

| 코드 | 조건 |
| --- | --- |
| `CATEGORY_DUPLICATED` | 같은 프리셋 또는 같은 이름의 활성 카테고리가 이미 있음 |
| `CATEGORY_PRESET_NOT_FOUND` | 존재하지 않는 프리셋 |

### 5.4 카테고리 수정

```http
PATCH /api/v1/categories/{categoryId}
```

Request:

```json
{
  "cycleDays": 14,
  "name": "욕실",
  "note": "물때와 습기를 기준으로 관리해요."
}
```

Response:

```json
{
  "data": {
    "id": "cat_bath",
    "name": "욕실",
    "cycleDays": 14,
    "lastDoneAt": "2026-07-01T09:00:00+09:00",
    "nextDueAt": "2026-07-15T09:00:00+09:00",
    "note": "물때와 습기를 기준으로 관리해요.",
    "status": {
      "code": "good",
      "label": "5일 뒤 다시 보면 충분해요",
      "daysUntilNext": 5
    }
  }
}
```

검증:

- `cycleDays`는 MVP에서 `3`, `7`, `14`, `21`, `28` 중 하나여야 한다.
- P1에서 직접 입력 주기를 허용할 수 있다.

### 5.5 카테고리 완료

```http
POST /api/v1/categories/{categoryId}/complete
```

Request:

```json
{
  "completedAt": "2026-07-10T12:00:00+09:00"
}
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

처리 규칙:

- 완료 처리는 트랜잭션으로 처리한다.
- `completion_logs`에 기록을 생성한다.
- `cleaning_categories.lastDoneAt`을 갱신한다.
- 응답은 갱신된 카테고리와 생성된 로그를 함께 반환한다.

### 5.6 완료 기록 조회

```http
GET /api/v1/completion-logs
```

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| limit | 아니오 | 기본 20, 최대 100 |
| cursor | 아니오 | 다음 페이지 커서 |
| from | 아니오 | 조회 시작일 |
| to | 아니오 | 조회 종료일 |

Response:

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
  "meta": {
    "nextCursor": null
  }
}
```

## 6. 알림

### 6.1 알림 목록 조회

```http
GET /api/v1/notifications
```

Response:

```json
{
  "data": [
    {
      "id": "noti_01",
      "categoryId": "cat_bath",
      "title": "이번 주에는 욕실만 챙겨도 충분해요",
      "body": "욕실 카테고리를 이번 주 안에 한 번 완료하면 다음 관리는 자동으로 다시 잡아둘게요.",
      "isRead": false,
      "createdAt": "2026-07-10T09:00:00+09:00"
    }
  ]
}
```

### 6.2 알림 읽음 처리

```http
POST /api/v1/notifications/{notificationId}/read
```

Response:

```json
{
  "data": {
    "id": "noti_01",
    "isRead": true,
    "readAt": "2026-07-10T12:00:00+09:00"
  }
}
```

## 7. 브랜드 셀렉션

### 7.1 셀렉션 목록 조회

```http
GET /api/v1/selections
```

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| category | 아니오 | `전체`, `욕실`, `주방`, `세탁/침구`, `쓰레기/수거`, `바닥/먼지`, `계절/가전` |
| type | 아니오 | `kit`, `product`, `service`, `subscription` |
| limit | 아니오 | 기본 20 |
| cursor | 아니오 | 다음 페이지 커서 |

Response:

```json
{
  "data": [
    {
      "id": "starter-kit",
      "type": "kit",
      "typeLabel": "키트",
      "category": "전체",
      "title": "자취 첫 달 기본 청소 키트",
      "label": "에디터 픽",
      "isHighlighted": true,
      "priceText": "19,000원대",
      "affiliateText": "일부 제휴",
      "reason": "세제 종류를 늘리기보다 매주 쓰는 것만 담았어요.",
      "fitFor": "처음 자취를 시작했거나 청소 도구가 거의 없는 사용자",
      "isSaved": true,
      "providers": []
    }
  ],
  "meta": {
    "nextCursor": null
  }
}
```

처리 규칙:

- `category=욕실` 요청 시 `category=욕실` 항목과 `category=전체` 항목을 함께 반환한다.
- `isHighlighted=true` 항목을 우선 정렬한다.
- 사용자 저장 여부를 `isSaved`로 내려준다.

### 7.2 셀렉션 상세 조회

```http
GET /api/v1/selections/{selectionId}
```

Response:

```json
{
  "data": {
    "id": "trash-pickup",
    "type": "subscription",
    "typeLabel": "구독 연결",
    "category": "쓰레기/수거",
    "title": "문앞 수거 서비스 3곳",
    "label": "반복 밀림에 추천",
    "isHighlighted": true,
    "priceText": "월 9,900원대부터",
    "affiliateText": "제휴 포함",
    "reason": "쓰레기 배출이 매번 밀리는 사람에게는 반복 수거 서비스가 더 직접적인 해결책이에요.",
    "fitFor": "퇴근 시간이 늦어 배출 요일을 자주 놓치는 사용자",
    "notice": "가격은 예시 또는 범위이며 외부 페이지에서 최종 확인해야 합니다.",
    "isSaved": false,
    "providers": [
      {
        "id": "provider_01",
        "name": "오늘수거",
        "ratingText": "4.8",
        "priceText": "월 9,900원대",
        "note": "정기 수거"
      }
    ]
  }
}
```

### 7.3 셀렉션 저장

```http
PUT /api/v1/selections/{selectionId}/save
```

Response:

```json
{
  "data": {
    "selectionId": "starter-kit",
    "isSaved": true,
    "savedAt": "2026-07-10T12:00:00+09:00"
  }
}
```

처리 규칙:

- 같은 항목 저장 요청은 멱등적으로 처리한다.

### 7.4 셀렉션 저장 해제

```http
DELETE /api/v1/selections/{selectionId}/save
```

Response `204`: 응답 본문 없음.

### 7.5 저장한 셀렉션 조회

```http
GET /api/v1/me/saved-selections
```

Response:

```json
{
  "data": [
    {
      "id": "starter-kit",
      "title": "자취 첫 달 기본 청소 키트",
      "category": "전체",
      "type": "kit",
      "priceText": "19,000원대",
      "affiliateText": "일부 제휴",
      "reason": "세제 종류를 늘리기보다 매주 쓰는 것만 담았어요.",
      "fitFor": "처음 자취를 시작했거나 청소 도구가 거의 없는 사용자",
      "isSaved": true
    }
  ]
}
```

### 7.6 외부 보기 클릭 기록

```http
POST /api/v1/selections/{selectionId}/external-view
```

Request:

```json
{
  "providerId": "provider_01"
}
```

Response:

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

MVP 규칙:

- 앱 내부에서 결제나 예약 확정을 처리하지 않는다.
- 실제 외부 URL이 준비되지 않은 초기 프로토타입에서는 `externalUrl`을 비워두고 CTA 클릭 이벤트만 기록할 수 있다.

## 8. 커뮤니티

### 8.1 커뮤니티 글 목록 조회

```http
GET /api/v1/community/posts
```

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| type | 예 | `tips`, `qa` |
| tag | 아니오 | 카테고리 태그 |
| limit | 아니오 | 기본 20 |
| cursor | 아니오 | 다음 페이지 커서 |

Response:

```json
{
  "data": [
    {
      "id": "tip1",
      "type": "tips",
      "title": "욕실은 세제보다 물기 제거가 먼저였어요",
      "tag": "욕실",
      "bodyPreview": "샤워 후 스퀴지로 30초만 닦아도 물때가 확 줄었습니다.",
      "helpfulCount": 128,
      "commentsCount": 24,
      "answersCount": 0,
      "savedCount": 64,
      "isPopular": true,
      "isSaved": false,
      "createdAt": "2026-07-10T09:00:00+09:00"
    }
  ],
  "meta": {
    "nextCursor": null
  }
}
```

처리 규칙:

- `helpfulCount + savedCount`를 기본 인기 정렬 기준으로 사용할 수 있다.
- 상위 글은 `isPopular=true`로 내려준다.

### 8.2 커뮤니티 글 상세 조회

```http
GET /api/v1/community/posts/{postId}
```

Response:

```json
{
  "data": {
    "id": "qa1",
    "type": "qa",
    "title": "대리석 세면대에 물때 제거제 써도 되나요?",
    "tag": "욕실",
    "body": "산성 세제는 표면을 상하게 할 수 있다는 답변이 가장 많이 도움을 받았어요.",
    "helpfulCount": 88,
    "commentsCount": 0,
    "answersCount": 6,
    "savedCount": 22,
    "isSaved": false,
    "hasMarkedHelpful": false,
    "createdAt": "2026-07-10T09:00:00+09:00"
  }
}
```

### 8.3 도움됨 표시

```http
PUT /api/v1/community/posts/{postId}/helpful
```

Response:

```json
{
  "data": {
    "postId": "tip1",
    "hasMarkedHelpful": true,
    "helpfulCount": 129
  }
}
```

처리 규칙:

- 사용자당 게시글 1회만 도움됨을 표시할 수 있다.
- 같은 요청은 멱등적으로 처리한다.

### 8.4 도움됨 취소

```http
DELETE /api/v1/community/posts/{postId}/helpful
```

Response:

```json
{
  "data": {
    "postId": "tip1",
    "hasMarkedHelpful": false,
    "helpfulCount": 128
  }
}
```

### 8.5 커뮤니티 글 저장

```http
PUT /api/v1/community/posts/{postId}/save
```

Response:

```json
{
  "data": {
    "postId": "tip1",
    "isSaved": true,
    "savedAt": "2026-07-10T12:00:00+09:00"
  }
}
```

### 8.6 커뮤니티 글 저장 해제

```http
DELETE /api/v1/community/posts/{postId}/save
```

Response `204`: 응답 본문 없음.

### 8.7 댓글 또는 답변 작성

```http
POST /api/v1/community/posts/{postId}/comments
```

Request:

```json
{
  "body": "중성세제로 먼저 테스트하는 게 안전했어요."
}
```

Response `201`:

```json
{
  "data": {
    "id": "comment_01",
    "postId": "qa1",
    "body": "중성세제로 먼저 테스트하는 게 안전했어요.",
    "createdAt": "2026-07-10T12:00:00+09:00"
  }
}
```

MVP 규칙:

- `tips` 글에서는 댓글로 표시한다.
- `qa` 글에서는 답변으로 표시한다.
- 초기 제품에서 작성 기능을 비활성화할 경우, 프론트엔드는 이 API 대신 작성 진입점 안내만 노출할 수 있다.

## 9. 마이

### 9.1 마이 요약 조회

```http
GET /api/v1/me/summary
```

Response:

```json
{
  "data": {
    "profile": {
      "id": "usr_01",
      "name": "김보송",
      "avatarText": "보"
    },
    "stats": {
      "monthlyCompletionCount": 5,
      "categoryCount": 6,
      "savedSelectionCount": 1
    },
    "weeklyFootprints": [
      {
        "weekStartDate": "2026-04-20",
        "completionCount": 1,
        "level": 1
      },
      {
        "weekStartDate": "2026-07-06",
        "completionCount": 2,
        "level": 2
      }
    ],
    "recentLogs": [
      {
        "id": "log_10",
        "categoryId": "cat_bath",
        "categoryName": "욕실",
        "completedAt": "2026-07-10T12:00:00+09:00"
      }
    ],
    "savedSelections": [
      {
        "id": "starter-kit",
        "title": "자취 첫 달 기본 청소 키트",
        "category": "전체",
        "type": "kit",
        "priceText": "19,000원대",
        "isSaved": true
      }
    ]
  }
}
```

처리 규칙:

- `weeklyFootprints`는 최근 12주를 반환한다.
- `level`은 주간 완료 수를 시각화하기 위한 값이다. 예: 0, 1, 2, 3.

## 10. 분석 이벤트

### 10.1 이벤트 기록

```http
POST /api/v1/analytics/events
```

Request:

```json
{
  "eventName": "category_completed",
  "properties": {
    "categoryId": "cat_bath",
    "cycleDays": 14
  },
  "occurredAt": "2026-07-10T12:00:00+09:00"
}
```

Response `204`: 응답 본문 없음.

필수 이벤트:

| 이벤트 | 발생 시점 |
| --- | --- |
| `category_completed` | 카테고리 완료 |
| `cycle_updated` | 주기 수정 |
| `selection_saved` | 셀렉션 저장 |
| `external_view_clicked` | 외부 보기 클릭 |
| `community_post_viewed` | 커뮤니티 글 조회 |
| `community_post_saved` | 커뮤니티 글 저장 |
| `notification_opened` | 알림 확인 |
| `app_revisited` | 재방문 |

## 11. 운영자 API

MVP 초기에는 정적 데이터 또는 간단한 CMS로 운영할 수 있다. 운영자 API가 필요한 경우 아래 범위만 우선 제공한다.

### 11.1 셀렉션 생성

```http
POST /api/v1/admin/selections
```

관리자 권한 필요.

Request:

```json
{
  "type": "product",
  "category": "욕실",
  "title": "욕실 물때 입문 세트",
  "label": "처음 쓰기 쉬움",
  "priceText": "9,000원대",
  "affiliateText": "제휴",
  "reason": "강한 세제보다 부드러운 솔과 물기 제거 도구를 먼저 쓰면 실패가 적어요.",
  "fitFor": "욕실 청소를 미루다가 한 번에 크게 하려는 사용자",
  "isHighlighted": true
}
```

### 11.2 커뮤니티 글 상태 변경

```http
PATCH /api/v1/admin/community/posts/{postId}
```

Request:

```json
{
  "status": "hidden",
  "isRecommended": false
}
```

## 12. 주요 검증 시나리오

### 12.1 카테고리 완료

```text
GET /home
-> POST /categories/{categoryId}/complete
-> GET /home
-> GET /me/summary
```

기대 결과:

- 완료한 카테고리의 `lastDoneAt`, `nextDueAt`이 갱신된다.
- 완료 기록이 생성된다.
- 월간 완료 수와 최근 기록이 증가한다.

### 12.2 도움 보기 후 셀렉션 저장

```text
GET /selections?category=욕실
-> GET /selections/{selectionId}
-> PUT /selections/{selectionId}/save
-> GET /me/saved-selections
```

기대 결과:

- 카테고리에 맞는 셀렉션이 노출된다.
- 저장 상태가 목록, 상세, 마이에 일관되게 반영된다.

### 12.3 커뮤니티 도움됨

```text
GET /community/posts?type=tips
-> GET /community/posts/{postId}
-> PUT /community/posts/{postId}/helpful
```

기대 결과:

- 게시글 상세가 표시된다.
- 도움됨 수가 1 증가한다.
- 같은 사용자의 중복 도움됨은 1회로 제한된다.

