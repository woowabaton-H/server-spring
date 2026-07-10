# CleanLoop 프론트엔드 API 통합 명세서 (v2 - 엄격 재검토)

> ⚠️ v1에서 `GET /category-presets`, `GET /me`를 "누락"으로 잘못 기재했었습니다.
> 코드를 직접 확인한 결과 **이미 백엔드에 구현되어 있고, 프론트에서 안 쓰고 있을 뿐**이었습니다.
> 이 문서는 실제 도메인 모델(record/DTO)까지 뒤져서 **정말로 없는 것**만 다시 정리했습니다.

---

## 🔴 진짜 없는 것 (도메인 모델 자체가 없음)

### 1. 댓글/답글 작성·조회 API — 전체 도메인 없음

**증거**:
- `CommunityPost` record (`CommunityPost.java`)에 `commentsCount`, `answersCount`는 **정수 카운터만** 존재
- 실제 댓글 텍스트를 저장하는 테이블/엔티티가 없음 (`CommunityReactionRepository`는 helpful/save 반응만 관리)
- `CommunityService`, `CommunityController`에 댓글 생성·조회 메서드가 전혀 없음
- `CommunityPostDetailResponse`에도 댓글 목록 필드 없음 (카운트만)

**프론트 현황**: `CleanLoopApp.tsx`의 `addCommunityReply()`, `post.answers: string[]`가 **클라이언트 상태에만 존재**. 새로고침하면 사라짐. 서버에 절대 반영되지 않음.

**필요한 API**:

```http
GET /api/v1/community/posts/{postId}/comments?cursor=&limit=
```
```json
{
  "data": [
    {
      "id": "cmt_001",
      "postId": "post_001",
      "authorName": "김보송",
      "authorIsMe": true,
      "body": "물때 제거제를 매일 쓰는 것보다 물기 제거를 먼저 해보세요.",
      "createdAt": "2026-07-10T12:00:00+09:00"
    }
  ],
  "meta": { "nextCursor": null }
}
```

```http
POST /api/v1/community/posts/{postId}/comments
Content-Type: application/json

{ "body": "저도 이 방법 써봤는데 효과 있었어요." }
```
```json
{
  "data": {
    "id": "cmt_002",
    "postId": "post_001",
    "authorName": "김보송",
    "authorIsMe": true,
    "body": "저도 이 방법 써봤는데 효과 있었어요.",
    "createdAt": "2026-07-10T13:00:00+09:00"
  }
}
```

**부가 요구사항**: 이 API가 생기면 `commentsCount`/`answersCount` 증감 로직도 함께 처리해야 함 (현재는 카운터만 있고 실제 삽입 로직 없음).

---

### 2. 커뮤니티 글 작성 API — 확인 결과 진짜 없음

**증거**: `CommunityController.java` 전체 메서드 목록 — `findAll`, `findOne`, `markHelpful`, `unmarkHelpful`, `save`, `unsave` 뿐. `POST /community/posts` 없음. `CommunityService.java`도 동일 확인.

**프론트 현황**: `openCommunityComposer()`가 `setCommunityPosts()`로 로컬 상태에만 새 글을 추가. 새로고침 시 소실.

**필요한 API**:
```http
POST /api/v1/community/posts
Content-Type: application/json

{
  "type": "tips",
  "title": "욕실 물때는 세제보다 물기 제거가 먼저입니다",
  "tag": "욕실",
  "body": "샤워 후 벽면과 거울의 물기를 바로 제거하면..."
}
```
> 응답 스키마는 `CommunityPostDetailResponse`와 동일하게 맞추면 됨.

---

### 3. 알림(Notification) 도메인 — 백엔드 주석에 명시적으로 "생략" 처리됨

**증거**: `HomeResponse.java` 최상단 주석:
```java
/**
 * unreadNotificationCount는 Notification 도메인이 없으므로 이번 범위에서 생략한다.
 */
```
즉 백엔드도 "일부러 안 만들었다"고 인정한 부분. `HomeResponse`에는 `unreadNotificationCount` 필드조차 없음.

**프론트 현황**: `hasUnreadNotification` state, `openNotification()`이 **오늘 기준 due/late 카테고리를 클라이언트에서 필터링**해서 흉내만 냄. 실제 "알림"이라는 개념(발송 시각, 읽음 처리, 알림 종류)이 서버에 전혀 없음.

**필요한 API** (신규 도메인):
```http
GET /api/v1/notifications
```
```json
{
  "data": [
    {
      "id": "noti_001",
      "categoryId": "cat_bath",
      "categoryName": "욕실",
      "message": "욕실 관리일이 지났어요",
      "isRead": false,
      "createdAt": "2026-07-10T09:00:00+09:00"
    }
  ],
  "meta": { "unreadCount": 1 }
}
```
```http
PUT /api/v1/notifications/read-all
```
(전체 읽음 처리 — 프론트가 알림함을 열 때 호출)

> 우선순위 낮음으로 봐도 됨: MVP라면 "홈에서 due/late 카테고리 필터링"을 그대로 클라이언트 로직으로 유지하고, 서버에 별도 알림 도메인을 만들지 않는 선택도 합리적임. 다만 현재는 "구현 예정"이 아니라 "설계에서 제외됨"이 명확하므로, 계속 쓸 거면 명세가 필요함.

---

### 4. 커뮤니티 글의 `steps`(진행 순서) 필드 — 도메인에 없음

**증거**: `CommunityPost` record 필드 전체 — `id, type, title, tag, body, helpfulCount, commentsCount, answersCount, savedCount, status, recommended, createdAt`. `steps` 관련 컬럼/필드 없음. `CommunityPostDetailResponse`에도 없음.

**프론트 현황**: `CommunityDetail`에서 `post.steps.map(...)`으로 "진행 순서" 섹션을 렌더링하지만 **완전히 더미 하드코딩**(`community` 객체 안에 박혀 있음). API로 대체하려면 백엔드에 필드 추가 필요.

**필요한 변경**: `CommunityPostDetailResponse`에 `steps: List<String>` 추가, 작성 API에도 `steps` 필드 추가.

---

### 5. 커뮤니티 글 ↔ 셀렉션 연결(`relatedSelectionIds`) — 관계 모델 없음

**증거**: `CommunityPost`, `CommunityPostDetailResponse` 어디에도 셀렉션 참조 필드 없음. `SelectionResponse`에도 역방향 참조 없음.

**프론트 현황**: `relatedSelectionsForCategory()`가 **카테고리 이름 문자열이 같은 셀렉션을 전부 매칭**하는 방식으로 흉내만 냄 (예: "세탁/침구" 글이면 "세탁/침구" 카테고리 셀렉션 전부를 "관련 셀렉션"으로 노출). 실제로는 특정 셀렉션만 골라서 연결하는 게 자연스러움.

**필요한 변경**: `CommunityPostDetailResponse`에 `relatedSelectionIds: List<String>` 추가 (또는 카테고리 매칭으로 충분하다면 이 항목은 기획 확인 후 스킵 가능 — API 문제가 아니라 **기획 결정이 필요한 부분**).

---

### 6. `SelectionResponse`에 프론트가 쓰는 필드 다수 누락

**증거**: `SelectionResponse.java` 필드 전체:
```
id, type, typeLabel, category, title, label, isHighlighted,
priceText, affiliateText, reason, fitFor, notice, isSaved, providers
```

**프론트가 실제로 쓰는 `SelectionItem` 필드**(`CleanLoopApp.tsx` 28-44줄):
```
image, rating, reviews, source, checks: string[], tags: string[]
```

| 프론트 필드 | 백엔드 대응 | 상태 |
|---|---|---|
| `image` | 없음 | ❌ 완전 누락 (상품 이미지) |
| `rating` | 없음 (provider에 `ratingText`만 있음, 최상위 없음) | ❌ 누락 |
| `reviews` | 없음 | ❌ 누락 |
| `tags` | 없음 | ❌ 누락 |
| `checks` | 없음 (`notice`는 단일 문자열, checks는 배열) | ❌ 구조 다름 |
| `source` | `providers[].name`으로 대체 가능하지만 목록에는 `providers`가 빈 배열(`ofListItem`에서 강제 `List.of()`) | ⚠️ 상세 조회에서만 가능 |

**결론**: 셀렉션 카드 UI(이미지, 별점, 후기 수, 태그)를 그대로 유지하려면 `SelectionResponse`에 `imageUrl`, `ratingText`, `reviewCountText`, `tags: List<String>` 추가가 필요함. 아니면 프론트 UI를 백엔드가 이미 주는 정보(`reason`, `fitFor`, `notice`)에 맞춰 축소해야 함 — **이건 API 설계 문제가 아니라 기획/디자인 결정 필요**.

---

## 🟡 착각했던 것 — 이미 존재하는 API (v1 문서 정정)

### ~~카테고리 프리셋 API~~ → 이미 있음

```java
// CategoryPresetControllerApiSpec.java, CategoryPresetController.java 존재 확인
GET /api/v1/category-presets
```
응답: `{ key, name, icon, cycleDays, note }[]`

**해야 할 일**: 프론트 `categoryApi`에 `findPresets()` 추가하고, `categoryPresets` 하드코딩 배열을 이 응답으로 교체. **백엔드 작업 없음, 프론트 배선만 필요.**

### ~~사용자 정보 API~~ → 이미 있음

```java
// UserControllerApiSpec.java 존재 확인
GET /api/v1/me
PATCH /api/v1/me
```
응답: `{ id, name, avatarText, timezone, createdAt }`

**해야 할 일**: `MyView`가 지금 `"보"`, `"김보송님의 기록"`을 하드코딩(`CleanLoopApp.tsx` 1397-1399줄) 하는데 이걸 `GET /me` 응답으로 교체. **백엔드 작업 없음, 프론트 배선만 필요.**

---

## 🟢 이미 API는 있는데 프론트가 안 쓰고 있는 것 (배선 누락)

| 더미/하드코딩 | 실제로는 어떤 API가 이미 있음 | 위치 |
|---|---|---|
| `MyView`의 `levels` 배열 (`[1,0,2,1,0,1,2,1,3,2,2,...]`, 12주 히트맵) | `GET /me/summary`의 `weeklyFootprints: [{weekStartDate, completionCount, level}]` | `CleanLoopApp.tsx` 1393줄 |
| `avatar`="보", 사용자 이름 하드코딩 | `GET /me` 또는 `GET /me/summary`의 `profile` | `CleanLoopApp.tsx` 1397줄 |
| `TODAY = new Date("2026-07-10...")` 고정 날짜 | `GET /home?today=` 파라미터로 서버 기준일 사용 가능 | `CleanLoopApp.tsx` 78줄 |
| 마이 메뉴 "주기 관리", "알림 설정" 버튼 | `onClick` 핸들러 자체가 없음 (dead button) — API 문제 아니라 프론트 미구현 | `CleanLoopApp.tsx` 1426-1427줄 |

> 이 항목들은 **백엔드에 요청할 필요 없이 프론트 작업만으로 해결 가능**합니다.

---

## 종합 우선순위

| 순위 | 항목 | 유형 | 이유 |
|---|---|---|---|
| 1 | 댓글/답글 작성·조회 | 🔴 신규 백엔드 필요 | 커뮤니티 상세의 핵심 상호작용인데 도메인이 없음 |
| 2 | 커뮤니티 글 작성 | 🔴 신규 백엔드 필요 | 새로고침하면 작성한 글이 날아감 |
| 3 | `weeklyFootprints`/`GET /me` 배선 | 🟢 프론트 작업만 | 이미 있는 API 안 쓰고 있음, 가장 빨리 고칠 수 있음 |
| 4 | `category-presets` 배선 | 🟢 프론트 작업만 | 이미 있는 API 안 쓰고 있음 |
| 5 | `SelectionResponse` 필드 확장 (image/rating/reviews/tags) | 🔴 신규 백엔드 필요 + 기획 결정 | UI를 유지할지 축소할지 먼저 결정 필요 |
| 6 | 커뮤니티 `steps` 필드 | 🔴 신규 백엔드 필요 | 우선순위 낮음, 없어도 핵심 기능엔 영향 적음 |
| 7 | 알림 도메인 | 🔴 신규 백엔드 필요 (설계 제외됨) | 백엔드가 의도적으로 뺀 범위. 필요하면 재논의 |
| 8 | 커뮤니티-셀렉션 관계 필드 | ⚪ 기획 결정 필요 | 카테고리 매칭으로 충분한지 먼저 확인 |
