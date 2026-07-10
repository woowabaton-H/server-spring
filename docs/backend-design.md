# 클린루프 백엔드 설계 문서

작성일: 2026-07-10  
기준 문서: `final/docs/functional-requirements.md`, `final/docs/api-spec.md`  
범위: MVP 백엔드 설계. 직접 판매, 결제, 외부 예약 확정, 고도화 추천은 제외한다.

## 1. 설계 목표

클린루프 백엔드는 사용자가 큰 청소 카테고리 단위로 관리 주기를 만들고, 완료 기록을 쌓고, 셀렉션과 커뮤니티를 통해 문제 해결 정보를 확인할 수 있도록 지원한다.

핵심 설계 목표:

1. 청소 카테고리 상태 계산을 서버에서 일관되게 처리한다.
2. 완료 처리와 완료 기록 생성을 하나의 트랜잭션으로 보장한다.
3. 셀렉션 저장, 커뮤니티 도움됨처럼 반복 클릭 가능한 액션은 멱등적으로 처리한다.
4. 가격, 제휴, 외부 연결 정보는 운영자가 투명하게 관리할 수 있게 한다.
5. MVP에서는 단순한 구조를 유지하되, 개인화 추천과 운영자 도구 확장에 필요한 데이터 구조는 막지 않는다.

## 2. 백엔드 구조도

```text
Client
  -> API Controller
  -> Domain Service
  -> Domain Repository
  -> Data Model
```

레이어별 책임:

| 레이어 | 책임 |
| --- | --- |
| API Controller | 요청 검증, 사용자 컨텍스트 확인, 응답 DTO 변환 |
| Domain Service | 도메인 규칙, 상태 계산, 트랜잭션 단위 처리 |
| Domain Repository | 도메인 객체 저장과 조회 |
| Data Model | 사용자, 카테고리, 완료 기록, 셀렉션, 커뮤니티, 알림 데이터 구조 |

## 3. 도메인 모듈

| 모듈 | 책임 |
| --- | --- |
| AuthModule | 게스트 세션 생성, 사용자 컨텍스트 해석 |
| UserModule | 사용자 프로필, 타임존, 마이 요약 |
| CategoryModule | 기본 카테고리 생성, 목록 조회, 주기 수정, 카테고리 추가 |
| CompletionModule | 완료 처리, 완료 기록 조회, 월간/주간 집계 |
| SelectionModule | 셀렉션 목록/상세, 저장/해제, 외부 보기 기록 |
| CommunityModule | 글 목록/상세, 도움됨, 저장, 댓글/답변 |
| NotificationModule | 알림 목록, 읽음 처리, 알림 후보 생성 |
| AnalyticsModule | 행동 이벤트 수집 |
| AdminModule | 셀렉션, 커뮤니티 상태, 카테고리 프리셋 관리 |

## 4. 주요 데이터 모델

### 4.1 users

사용자 정보. MVP에서는 게스트 사용자도 `users`에 저장한다.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 사용자 ID |
| name | varchar(40) | 표시 이름 |
| avatar_text | varchar(4) | 아바타 문자 |
| timezone | varchar(64) | 기본 `Asia/Seoul` |
| device_id_hash | varchar(128) nullable | 게스트 재식별용 해시 |
| role | varchar(20) | `user`, `admin` |
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

인덱스:

- `idx_users_device_id_hash`

### 4.2 cleaning_categories

사용자가 관리하는 청소 카테고리.

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

인덱스:

- `idx_categories_user_active_sort(user_id, is_active, sort_order)`
- `idx_categories_user_preset(user_id, preset_key)`

### 4.3 category_presets

추천 카테고리 프리셋.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| preset_key | varchar(50) pk | 프리셋 키. `key`는 H2 예약어라 컬럼명만 다르고, API 응답 필드는 `key`다 |
| name | varchar(40) | 이름 |
| icon | varchar(40) | 아이콘 키 |
| cycle_days | int | 기본 주기 |
| note | text | 기본 설명 |
| sort_order | int | 노출 순서 |
| is_default | boolean | 신규 사용자 기본 생성 여부 |
| is_active | boolean | 활성 여부 |

초기 기본 프리셋:

| key | name | cycle_days |
| --- | --- | --- |
| bath | 욕실 | 14 |
| kitchen | 주방 | 7 |
| laundry | 세탁/침구 | 14 |
| trash | 쓰레기/수거 | 3 |
| floor | 바닥/먼지 | 7 |
| season | 계절/가전 | 28 |
| pet | 반려동물 | 7 |

### 4.4 completion_logs

청소 완료 기록.

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
- `idx_completion_logs_user_month(user_id, completed_at)`
- `idx_completion_logs_category(category_id, completed_at desc)`

### 4.5 selection_items

브랜드 셀렉션 항목.

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
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

인덱스:

- `idx_selection_items_status_category(status, category, is_highlighted, sort_order)`
- `idx_selection_items_type(type)`

### 4.6 provider_options

서비스/구독형 셀렉션에 연결되는 제공업체 후보.

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
| is_active | boolean | 활성 여부 |

인덱스:

- `idx_provider_options_selection(selection_item_id, sort_order)`

### 4.7 saved_selections

사용자가 저장한 셀렉션.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 저장 ID |
| user_id | uuid fk users.id | 사용자 |
| selection_item_id | uuid fk selection_items.id | 셀렉션 |
| created_at | timestamptz | 저장 시각 |

제약:

- `unique(user_id, selection_item_id)`

인덱스:

- `idx_saved_selections_user_created(user_id, created_at desc)`

### 4.8 community_posts

커뮤니티 글.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 글 ID |
| type | varchar(20) | `tips`, `qa` |
| title | varchar(160) | 제목 |
| tag | varchar(40) | 카테고리 태그 |
| body | text | 본문 |
| author_id | uuid nullable | 작성자. 운영 콘텐츠는 nullable 가능 |
| helpful_count | int | 도움됨 수 집계값 |
| comments_count | int | 댓글 수 집계값 |
| answers_count | int | 답변 수 집계값 |
| saved_count | int | 저장 수 집계값 |
| status | varchar(20) | `published`, `hidden` |
| is_recommended | boolean | 운영 추천 여부 |
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

인덱스:

- `idx_community_posts_type_status(type, status, created_at desc)`
- `idx_community_posts_popular(type, status, helpful_count desc, saved_count desc)`
- `idx_community_posts_tag(tag)`

### 4.9 community_reactions

커뮤니티 글의 도움됨 또는 저장 반응.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 반응 ID |
| user_id | uuid fk users.id | 사용자 |
| post_id | uuid fk community_posts.id | 글 |
| reaction_type | varchar(20) | `helpful`, `save` |
| created_at | timestamptz | 생성 시각 |

제약:

- `unique(user_id, post_id, reaction_type)`

인덱스:

- `idx_community_reactions_user(user_id, reaction_type, created_at desc)`
- `idx_community_reactions_post(post_id, reaction_type)`

### 4.10 community_comments

댓글 또는 답변.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 댓글 ID |
| post_id | uuid fk community_posts.id | 글 |
| user_id | uuid fk users.id | 작성자 |
| body | text | 내용 |
| status | varchar(20) | `published`, `hidden` |
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

인덱스:

- `idx_community_comments_post_created(post_id, created_at)`

### 4.11 notifications

알림.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 알림 ID |
| user_id | uuid fk users.id | 사용자 |
| category_id | uuid nullable | 연결 카테고리 |
| title | varchar(120) | 제목 |
| body | text | 본문 |
| deep_link | varchar(200) nullable | 앱 내부 이동 경로 |
| is_read | boolean | 읽음 여부 |
| created_at | timestamptz | 생성 시각 |
| read_at | timestamptz nullable | 읽은 시각 |

인덱스:

- `idx_notifications_user_read_created(user_id, is_read, created_at desc)`

### 4.12 analytics_events

행동 분석 이벤트.

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 이벤트 ID |
| user_id | uuid nullable | 사용자 |
| event_name | varchar(80) | 이벤트명 |
| properties | jsonb | 속성 |
| occurred_at | timestamptz | 발생 시각 |
| created_at | timestamptz | 적재 시각 |

인덱스:

- `idx_analytics_events_name_occurred(event_name, occurred_at desc)`
- `idx_analytics_events_user_occurred(user_id, occurred_at desc)`

## 5. 도메인 로직

### 5.1 카테고리 상태 계산

입력:

- `last_done_at`
- `cycle_days`
- 사용자 타임존 기준 `today`

계산:

```text
next_due_at = last_done_at + cycle_days
days_until_next = ceil((next_due_at - now) / 1 day)
```

상태:

| 조건 | code | label |
| --- | --- | --- |
| `days_until_next <= 0` | `due` | `이번 주에 챙기면 좋아요` |
| `days_until_next <= 2` | `soon` | `{N}일 안에 하면 좋아요` |
| 그 외 | `good` | `{N}일 뒤 다시 보면 충분해요` |

주의:

- 상태는 영속 데이터에 저장하지 않고 조회 시 계산한다.
- `next_due_at`도 저장하지 않는 것을 기본으로 한다.

### 5.2 기본 카테고리 생성

게스트 세션 생성 또는 신규 사용자 생성 시:

1. `category_presets.is_default=true` 목록을 조회한다.
2. 사용자별 `cleaning_categories`를 생성한다.
3. `last_done_at`은 가입 시각 또는 프리셋별 기준 시각으로 세팅한다.
4. 중복 요청을 대비해 `user_id + preset_key` 유니크 제약으로 보호한다.

### 5.3 완료 처리 트랜잭션

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

중복 완료 정책:

- MVP에서는 같은 날 같은 카테고리의 여러 완료 기록을 허용할 수 있다.
- 중복 방지가 필요해지면 `user_id, category_id, completed_date` 유니크 인덱스를 추가하고 클라이언트 확인 UI를 붙인다.

### 5.4 주기 수정

검증:

- MVP 허용값: `3`, `7`, `14`, `21`, `28`
- 카테고리 소유자만 수정 가능
- 수정 후 상태는 재계산해 응답한다.

분석:

- 수정 전/후 `cycle_days`를 `cycle_updated` 이벤트로 기록한다.

### 5.5 셀렉션 목록 정렬

기본 정렬:

```text
is_highlighted desc
sort_order asc
created_at desc
```

카테고리 필터:

- `category=전체` 요청: 전체 공개 항목 모두 반환
- 특정 카테고리 요청: `category in ('전체', 요청 카테고리)` 반환

저장 상태:

- 로그인 사용자 기준 `saved_selections`를 left join해 `isSaved`를 계산한다.

### 5.6 셀렉션 저장

저장:

```text
insert into saved_selections(user_id, selection_item_id)
on conflict do nothing
```

저장 해제:

```text
delete from saved_selections
where user_id = :userId and selection_item_id = :selectionItemId
```

### 5.7 커뮤니티 인기 정렬

MVP 정렬 점수:

```text
popular_score = helpful_count + saved_count
```

확장 가능 항목:

- 최신성 가중치
- 운영 추천 가중치
- 신고 또는 숨김 상태 제외

### 5.8 커뮤니티 도움됨

도움됨 추가:

1. `community_reactions`에 `reaction_type=helpful`을 멱등 삽입한다.
2. 새로 삽입된 경우에만 `community_posts.helpful_count`를 1 증가시킨다.

도움됨 취소:

1. 반응 row를 삭제한다.
2. 삭제된 경우에만 `helpful_count`를 1 감소시킨다.
3. 카운트는 0 미만이 되지 않도록 보호한다.

### 5.9 마이 요약

`GET /me/summary`는 여러 데이터를 조합한다.

| 값 | 계산 |
| --- | --- |
| monthlyCompletionCount | 사용자 타임존 기준 이번 달 완료 기록 수 |
| categoryCount | 활성 카테고리 수 |
| savedSelectionCount | 저장한 셀렉션 수 |
| weeklyFootprints | 최근 12주 주간 완료 기록 집계 |
| recentLogs | 최근 완료 기록 최신순 |
| savedSelections | 저장한 셀렉션 최신순 |

집계 방식:

- MVP에서는 실시간 집계로 충분하다.
- 데이터가 늘어나면 주간/월간 집계 테이블을 추가한다.

## 6. API 서비스 구조

권장 서비스 레이어:

```text
controllers/
  auth.controller
  home.controller
  categories.controller
  selections.controller
  community.controller
  notifications.controller
  me.controller
  analytics.controller

services/
  auth.service
  home.service
  category.service
  completion.service
  selection.service
  community.service
  notification.service
  analytics.service

repositories/
  user.repository
  category.repository
  completion-log.repository
  selection.repository
  community.repository
  notification.repository
```

원칙:

- Controller는 요청 검증과 응답 DTO 변환만 담당한다.
- Service는 도메인 규칙과 트랜잭션을 담당한다.
- Repository는 도메인 데이터 저장과 조회만 담당한다.
- 상태 문구 생성은 `CategoryStatusService` 같은 순수 함수로 분리한다.

## 7. 권한 모델

| 리소스 | 사용자 권한 |
| --- | --- |
| User | 본인만 조회/수정 |
| CleaningCategory | 본인 소유 카테고리만 조회/수정/완료 |
| CompletionLog | 본인 기록만 조회 |
| SavedSelection | 본인 저장 항목만 조회/변경 |
| SelectionItem | 공개 상태 항목은 모든 인증 사용자 조회 |
| CommunityPost | 공개 상태 글은 모든 인증 사용자 조회 |
| CommunityReaction | 본인 반응만 생성/삭제 |
| Notification | 본인 알림만 조회/읽음 처리 |
| Admin API | `role=admin`만 접근 |

## 8. 시드 데이터

### 8.1 기본 카테고리 프리셋

신규 사용자 생성 시 아래 카테고리를 기본 생성한다.

| 이름 | icon | cycleDays | note |
| --- | --- | --- | --- |
| 욕실 | bath | 14 | 물때와 습기만 잡아도 관리가 쉬워져요. |
| 주방 | kitchen | 7 | 배수구와 조리대 표면을 기준으로 잡아요. |
| 세탁/침구 | laundry | 14 | 침구와 수건을 같은 리듬으로 관리해요. |
| 쓰레기/수거 | trash | 3 | 배달이 많은 주에는 조금 짧게 잡아도 좋아요. |
| 바닥/먼지 | floor | 7 | 머리카락과 먼지를 먼저 잡는 카테고리예요. |
| 계절/가전 | season | 28 | 에어컨 필터, 제습, 결로처럼 계절에 따라 챙겨요. |

추가 프리셋:

| 이름 | icon | cycleDays | note |
| --- | --- | --- | --- |
| 반려동물 | floor | 7 | 털, 냄새, 패드 주변을 한 카테고리로 관리해요. |

### 8.2 셀렉션 초기 데이터

프로토타입에 있는 항목을 초기 셀렉션 데이터로 사용한다.

| slug | type | category | title |
| --- | --- | --- | --- |
| starter-kit | kit | 전체 | 자취 첫 달 기본 청소 키트 |
| bath-soft-start | product | 욕실 | 욕실 물때 입문 세트 |
| sink-weekly | product | 주방 | 싱크대 주간 관리 팩 |
| laundry-bedding | service | 세탁/침구 | 침구 수거 서비스 비교 |
| trash-pickup | subscription | 쓰레기/수거 | 문앞 수거 서비스 3곳 |
| floor-easy | product | 바닥/먼지 | 롤클리너와 극세사 조합 |
| season-aircon | service | 계절/가전 | 에어컨 필터와 분해청소 기준 |

### 8.3 커뮤니티 초기 데이터

프로토타입에 있는 꿀팁과 Q&A를 초기 운영 콘텐츠로 사용한다.

| type | tag | 예시 제목 |
| --- | --- | --- |
| tips | 욕실 | 욕실은 세제보다 물기 제거가 먼저였어요 |
| tips | 수거 | 음식물 쓰레기는 냄새 잡기보다 주기를 줄이는 게 답 |
| tips | 바닥 | 물걸레 전에 롤클리너 한 번이면 두 번 일 안 해요 |
| tips | 세탁 | 이불 세탁은 세탁보다 완전 건조가 핵심 |
| qa | 욕실 | 대리석 세면대에 물때 제거제 써도 되나요? |
| qa | 세탁 | 수건 냄새가 세탁 후에도 남을 때 뭘 먼저 봐야 하나요? |
| qa | 수거 | 분리수거함은 몇 칸짜리가 현실적으로 좋나요? |

## 9. 운영 정책

### 9.1 셀렉션

- 추천 이유와 적합한 상황을 필수 입력으로 둔다.
- 제휴 여부는 필수 입력으로 둔다.
- 가격은 고정 금액 보장처럼 표현하지 않고 `가격대`, `예시`, `범위`로 관리한다.
- 외부 URL이 있는 경우에도 최종 조건 확인 고지를 함께 노출한다.

### 9.2 커뮤니티

- `status=hidden` 글은 사용자 API에서 제외한다.
- 위험한 청소 방법, 검증되지 않은 과장 정보, 광고성 글은 숨김 처리한다.
- 좋은 글은 `is_recommended=true` 또는 높은 반응 점수로 더 쉽게 노출한다.

### 9.3 알림

- MVP에서는 앱 내부 알림만 제공한다.
- 실제 푸시 알림은 P2 확장이다.
- 알림 문구는 압박형 표현을 피한다.

## 10. 데이터 접근 정책

| 영역 | 정책 |
| --- | --- |
| 게스트 식별 | 원본 deviceId를 저장하지 않고 해시 저장 |
| 소유권 검증 | 사용자별 리소스 접근 시 항상 소유 사용자 기준으로 검증 |
| 관리자 작업 | 셀렉션, 커뮤니티 숨김, 프리셋 변경 같은 운영 작업은 관리자 권한에서만 허용 |
| 분석 이벤트 | 개인정보를 properties에 직접 넣지 않도록 제한 |
| 외부 URL | 허용 도메인 또는 운영자 검수 URL만 노출 |

## 11. 조회 및 집계 설계

초기 MVP 예상 데이터 규모에서는 요청 시점 계산과 단순 집계로 충분하다. 단, 화면에서 반복적으로 필요한 값은 계산 규칙을 명확히 둔다.

주요 조회 기준:

| 데이터 | 정렬/계산 기준 |
| --- | --- |
| 카테고리 목록 | 사용자별 활성 카테고리, `sort_order` 오름차순 |
| 완료 기록 | 사용자별 `completed_at` 최신순 |
| 이번 달 완료 수 | 사용자 타임존 기준 현재 월의 완료 기록 수 |
| 최근 12주 기록 | 주 시작일 기준 완료 기록 수를 12개 구간으로 집계 |
| 저장 셀렉션 | 사용자별 저장 시각 최신순 |
| 커뮤니티 인기 목록 | `helpful_count + saved_count` 높은 순 |
| 알림 목록 | 사용자별 읽음 여부, 생성 시각 최신순 |

## 12. 트랜잭션과 동시성

| 기능 | 처리 |
| --- | --- |
| 카테고리 완료 | 카테고리 row lock 후 로그 생성과 lastDoneAt 갱신 |
| 카테고리 추가 | 사용자별 이름/프리셋 유니크 제약으로 중복 방지 |
| 셀렉션 저장 | unique 제약 + upsert |
| 도움됨 표시 | unique 제약 + 카운터 조건부 증가 |
| 저장 해제 | 삭제된 row 수가 1일 때만 카운터 감소 |

## 13. 주기 스케줄러

MVP 초기에는 카테고리 상태를 요청 시 계산한다. 앱 내부 알림과 주간 기록을 자동으로 준비해야 하는 시점부터 아래 스케줄러를 둔다.

| 스케줄러 | 목적 | 실행 기준 |
| --- | --- | --- |
| due-category-notification | 챙길 시점이 된 카테고리의 앱 내부 알림 생성 | 매일 오전, 사용자 타임존 기준 |
| weekly-footprint-rollup | 최근 12주 표시용 주간 완료 수 집계 | 매주 월요일 |

### 13.1 챙길 카테고리 알림 생성 규칙

1. 활성 카테고리의 `last_done_at + cycle_days`를 계산한다.
2. 계산된 다음 관리일이 오늘이거나 지난 카테고리를 후보로 잡는다.
3. 같은 카테고리에 대해 읽지 않은 알림이 이미 있으면 새 알림을 만들지 않는다.
4. 알림 문구는 압박형 표현을 피하고, `이번 주에는 {카테고리}만 챙겨도 충분해요`처럼 제안형으로 만든다.
5. 사용자가 카테고리를 완료하면 해당 카테고리의 미확인 알림은 읽음 또는 만료 처리할 수 있다.

### 13.2 주간 완료 기록 집계 규칙

1. 사용자 타임존 기준 주 시작일을 계산한다.
2. 최근 12주 구간의 완료 기록 수를 집계한다.
3. 화면 표시용 `level`은 완료 수에 따라 0~3으로 변환한다.
4. 집계 데이터가 없어도 최근 12주 구간은 빈 값으로 반환할 수 있어야 한다.

## 14. 테스트 전략

### 14.1 단위 테스트

| 대상 | 검증 |
| --- | --- |
| CategoryStatusService | due, soon, good 계산 |
| CompletionService | 완료 처리 후 날짜/로그 갱신 |
| SelectionService | 카테고리 필터, 저장 멱등성 |
| CommunityService | 도움됨 중복 방지, 카운터 증가/감소 |
| MySummaryService | 월간 수, 최근 12주 집계 |

### 14.2 통합 테스트

필수 시나리오:

1. 게스트 세션 생성 시 기본 카테고리가 생성된다.
2. 카테고리를 완료하면 로그와 `last_done_at`이 함께 갱신된다.
3. 주기를 수정하면 다음 관리일과 상태 문구가 바뀐다.
4. 셀렉션 저장/해제 상태가 목록, 상세, 마이에 반영된다.
5. 커뮤니티 도움됨은 사용자당 1회만 반영된다.
6. 알림 읽음 처리 후 미확인 카운트가 줄어든다.

### 14.3 계약 테스트

프론트엔드 프로토타입이 기대하는 필드:

- `category.status.label`
- `category.nextDueAt`
- `selection.isSaved`
- `selection.providers`
- `communityPost.isPopular`
- `me.summary.weeklyFootprints`

API 응답에서 위 필드가 빠지지 않도록 계약 테스트를 둔다.

## 15. 개발 순서

| 순서 | 작업 |
| --- | --- |
| 1 | 데이터 모델 생성, 시드 데이터, 게스트 세션 |
| 2 | 카테고리 목록, 홈 요약, 상태 계산 |
| 3 | 완료 처리, 완료 기록, 마이 요약 |
| 4 | 주기 수정, 카테고리 추가 |
| 5 | 셀렉션 목록/상세, 저장/해제 |
| 6 | 커뮤니티 목록/상세, 도움됨, 저장 |
| 7 | 알림 목록/읽음, 앱 내부 알림 생성 |
| 8 | 분석 이벤트 기록 |
| 9 | 운영자 최소 API |

## 16. MVP 완료 기준

백엔드 MVP는 아래 조건을 만족하면 완료로 본다.

1. 신규 사용자에게 기본 청소 카테고리가 생성된다.
2. 홈 API가 카테고리 상태, 월간 완료 수, 최근 기록을 반환한다.
3. 완료 API가 로그 생성과 카테고리 갱신을 트랜잭션으로 처리한다.
4. 주기 수정과 카테고리 추가가 가능하다.
5. 셀렉션 목록/상세/저장/해제가 가능하다.
6. 커뮤니티 목록/상세/도움됨이 가능하다.
7. 마이 요약이 완료 수, 카테고리 수, 저장 수, 최근 기록, 저장 셀렉션을 반환한다.
8. 알림 조회와 읽음 처리가 가능하다.
9. 핵심 분석 이벤트를 저장할 수 있다.
10. 직접 판매, 결제, 외부 예약 확정은 API 범위에 포함하지 않는다.
