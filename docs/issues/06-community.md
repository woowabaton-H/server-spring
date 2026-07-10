# 6. Community 도메인

작업 순서: 6번째
의존성: User
범위: 커뮤니티 글 조회, 인기 정렬, 도움됨/저장. 글 원본 데이터는 별도 SQL로 시딩하므로 이번 범위에서 제외한다. 댓글/답변 작성 API는 이번 범위에서 제외한다(향후 확장).

## 6.1 데이터 구조

### community_posts

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 글 ID |
| type | varchar(20) | `tips`, `qa` |
| title | varchar(160) | 제목 |
| tag | varchar(40) | 카테고리 태그 |
| body | text | 본문 |
| helpful_count | int | 도움됨 수 집계값 |
| comments_count | int | 댓글 수 집계값 |
| answers_count | int | 답변 수 집계값 |
| saved_count | int | 저장 수 집계값 |
| status | varchar(20) | `published`, `hidden` |
| is_recommended | boolean | 운영 추천 여부 |
| created_at | timestamptz | 생성 시각 |

인덱스:

- `idx_community_posts_type_status(type, status, created_at desc)`
- `idx_community_posts_popular(type, status, helpful_count desc, saved_count desc)`

### community_reactions

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 반응 ID |
| user_id | uuid fk users.id | 사용자 |
| post_id | uuid fk community_posts.id | 글 |
| reaction_type | varchar(20) | `helpful`, `save` |
| created_at | timestamptz | 생성 시각 |

제약: `unique(user_id, post_id, reaction_type)`

## 6.2 도메인 로직

### 인기 정렬

```text
popular_score = helpful_count + saved_count
```

상위 글은 `isPopular=true`로 응답에 포함한다.

### 도움됨 처리

추가:

1. `community_reactions`에 `reaction_type=helpful`을 멱등 삽입한다.
2. 새로 삽입된 경우에만 `helpful_count`를 1 증가시킨다.

취소:

1. 반응 row를 삭제한다.
2. 삭제된 경우에만 `helpful_count`를 1 감소시킨다. 0 미만이 되지 않도록 보호한다.

저장(save)도 동일한 멱등 패턴을 따른다.

## 6.3 API 명세

### GET /community/posts

Query: `type`(필수: `tips`|`qa`), `tag`(선택), `limit`, `cursor`

Response `200`:

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
  "meta": { "nextCursor": null }
}
```

### GET /community/posts/{postId}

Response `200`: 목록 항목 필드 + `body`(전문), `hasMarkedHelpful`.

### PUT /community/posts/{postId}/helpful

Response `200`:

```json
{ "data": { "postId": "tip1", "hasMarkedHelpful": true, "helpfulCount": 129 } }
```

### DELETE /community/posts/{postId}/helpful

Response `200`:

```json
{ "data": { "postId": "tip1", "hasMarkedHelpful": false, "helpfulCount": 128 } }
```

### PUT /community/posts/{postId}/save

Response `200`:

```json
{ "data": { "postId": "tip1", "isSaved": true, "savedAt": "2026-07-10T12:00:00+09:00" } }
```

### DELETE /community/posts/{postId}/save

Response `204`.

## 6.4 완료 기준

1. `type`/`tag` 필터로 글 목록을 조회할 수 있고 인기순 정렬이 반영된다.
2. 상세 조회 시 `hasMarkedHelpful` 등 사용자별 상태가 포함된다.
3. 도움됨 표시/취소가 사용자당 1회로 제한되며 카운터가 조건부로 증감한다.
4. 저장/해제가 동일한 멱등 규칙으로 동작한다.
