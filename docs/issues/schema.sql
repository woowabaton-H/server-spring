-- ================================================================
-- 클린루프 (CleanLoop) H2 데이터베이스 스키마 + 더미 데이터
-- 대상: H2 (로컬/테스트 DB)
-- 구성: 01~06 도메인 문서에서 정의한 테이블 구조를 그대로 반영
-- 사용법: H2 콘솔 또는 datasource 초기화 시 이 파일 전체를 실행
--         Spring Boot schema.sql / data.sql로 나누고 싶으면
--         "DUMMY DATA" 표시 위/아래로 파일을 자르면 됩니다.
-- 주의: last_done_at / completed_at은 DATEADD로 "오늘 기준 N일 전"을
--       계산하므로, 실행 시점과 무관하게 상태(due/soon/good)가
--       재현됩니다.
-- H2 제약: category_presets의 PK 컬럼명은 KEY가 H2 예약어라 preset_key로 둡니다.
--          (API 응답 필드명은 그대로 `key`입니다.)
--          UUID 컬럼은 DEFAULT 절이 PRIMARY KEY보다 앞에 와야 합니다.
-- ================================================================

-- ----------------------------------------------------------------
-- 0. 기존 테이블 정리 (재실행 대비, 의존성 역순)
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS community_reactions;
DROP TABLE IF EXISTS community_posts;
DROP TABLE IF EXISTS saved_selections;
DROP TABLE IF EXISTS provider_options;
DROP TABLE IF EXISTS selection_items;
DROP TABLE IF EXISTS completion_logs;
DROP TABLE IF EXISTS cleaning_categories;
DROP TABLE IF EXISTS category_presets;
DROP TABLE IF EXISTS users;

-- ================================================================
-- 1. TABLE 선언 (01-user.md)
-- ================================================================

CREATE TABLE users (
                       id           UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                       name         VARCHAR(40) NOT NULL,
                       avatar_text  VARCHAR(4),
                       timezone     VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
                       created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- 2. TABLE 선언 (02-category.md)
-- ================================================================

CREATE TABLE category_presets (
                                  preset_key  VARCHAR(50) PRIMARY KEY,
                                  name        VARCHAR(40) NOT NULL,
                                  icon        VARCHAR(40) NOT NULL,
                                  cycle_days  INT NOT NULL CHECK (cycle_days > 0),
                                  note        VARCHAR(500),
                                  sort_order  INT NOT NULL DEFAULT 0,
                                  is_default  BOOLEAN NOT NULL DEFAULT FALSE,
                                  is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cleaning_categories (
                                     id           UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                     user_id      UUID NOT NULL REFERENCES users(id),
                                     preset_key   VARCHAR(50) REFERENCES category_presets(preset_key),
                                     name         VARCHAR(40) NOT NULL,
                                     icon         VARCHAR(40) NOT NULL,
                                     cycle_days   INT NOT NULL CHECK (cycle_days > 0),
                                     last_done_at TIMESTAMP NOT NULL,
                                     note         VARCHAR(500),
                                     sort_order   INT NOT NULL DEFAULT 0,
                                     is_active    BOOLEAN NOT NULL DEFAULT TRUE,
                                     created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 참고: "같은 사용자 내 활성 이름/프리셋 중복 방지"는 부분 유니크 제약
-- 대신 애플리케이션(Service) 레벨에서 검증한다고 가정하고 조회용
-- 인덱스만 둔다.
CREATE INDEX idx_categories_user_active_sort ON cleaning_categories(user_id, is_active, sort_order);
CREATE INDEX idx_categories_user_preset ON cleaning_categories(user_id, preset_key);

-- ================================================================
-- 3. TABLE 선언 (03-completion.md)
-- ================================================================

CREATE TABLE completion_logs (
                                 id            UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                 user_id       UUID NOT NULL REFERENCES users(id),
                                 category_id   UUID REFERENCES cleaning_categories(id),
                                 category_name VARCHAR(40) NOT NULL,
                                 completed_at  TIMESTAMP NOT NULL,
                                 created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_completion_logs_user_completed ON completion_logs(user_id, completed_at DESC);
CREATE INDEX idx_completion_logs_category ON completion_logs(category_id, completed_at DESC);

-- ================================================================
-- 4. TABLE 선언 (05-selection.md)
-- ================================================================

CREATE TABLE selection_items (
                                 id              UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                 slug            VARCHAR(80) NOT NULL UNIQUE,
                                 type            VARCHAR(30) NOT NULL,
                                 category        VARCHAR(40) NOT NULL,
                                 title           VARCHAR(120) NOT NULL,
                                 label           VARCHAR(40),
                                 price_text      VARCHAR(80),
                                 affiliate_text  VARCHAR(40),
                                 reason          VARCHAR(1000),
                                 fit_for         VARCHAR(500),
                                 notice          VARCHAR(500),
                                 is_highlighted  BOOLEAN NOT NULL DEFAULT FALSE,
                                 external_url    VARCHAR(500),
                                 status          VARCHAR(20) NOT NULL DEFAULT 'published',
                                 sort_order      INT NOT NULL DEFAULT 0,
                                 created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_selection_items_status_category ON selection_items(status, category, is_highlighted, sort_order);
CREATE INDEX idx_selection_items_type ON selection_items(type);

CREATE TABLE provider_options (
                                  id                 UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                  selection_item_id  UUID NOT NULL REFERENCES selection_items(id),
                                  name               VARCHAR(80) NOT NULL,
                                  rating_text        VARCHAR(20),
                                  price_text         VARCHAR(80),
                                  note               VARCHAR(120),
                                  external_url       VARCHAR(500),
                                  sort_order         INT NOT NULL DEFAULT 0,
                                  is_active          BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_provider_options_selection ON provider_options(selection_item_id, sort_order);

CREATE TABLE saved_selections (
                                  id                 UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                  user_id            UUID NOT NULL REFERENCES users(id),
                                  selection_item_id  UUID NOT NULL REFERENCES selection_items(id),
                                  created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT uq_saved_selections UNIQUE (user_id, selection_item_id)
);
CREATE INDEX idx_saved_selections_user_created ON saved_selections(user_id, created_at DESC);

-- ================================================================
-- 5. TABLE 선언 (06-community.md)
-- ================================================================

CREATE TABLE community_posts (
                                 id               UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                 type             VARCHAR(20) NOT NULL,
                                 title            VARCHAR(160) NOT NULL,
                                 tag              VARCHAR(40),
                                 body             VARCHAR(2000) NOT NULL,
                                 helpful_count    INT NOT NULL DEFAULT 0,
                                 comments_count   INT NOT NULL DEFAULT 0,
                                 answers_count    INT NOT NULL DEFAULT 0,
                                 saved_count      INT NOT NULL DEFAULT 0,
                                 status           VARCHAR(20) NOT NULL DEFAULT 'published',
                                 is_recommended   BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_community_posts_type_status ON community_posts(type, status, created_at DESC);
CREATE INDEX idx_community_posts_popular ON community_posts(type, status, helpful_count DESC, saved_count DESC);

CREATE TABLE community_reactions (
                                     id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                                     user_id        UUID NOT NULL REFERENCES users(id),
                                     post_id        UUID NOT NULL REFERENCES community_posts(id),
                                     reaction_type  VARCHAR(20) NOT NULL,
                                     created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT uq_community_reactions UNIQUE (user_id, post_id, reaction_type)
);
CREATE INDEX idx_community_reactions_user ON community_reactions(user_id, reaction_type, created_at DESC);
CREATE INDEX idx_community_reactions_post ON community_reactions(post_id, reaction_type);


-- ================================================================
-- ▼▼▼ DUMMY DATA (필요 시 이 아래만 잘라서 data.sql로 사용) ▼▼▼
-- ================================================================

-- ----------------------------------------------------------------
-- 데모 사용자 1명 (인증 없이 고정 컨텍스트로 사용)
-- ----------------------------------------------------------------
INSERT INTO users (id, name, avatar_text, timezone) VALUES
    ('a0000000-0000-0000-0000-000000000001', '보송', '보', 'Asia/Seoul');

-- ----------------------------------------------------------------
-- 카테고리 프리셋 7종 (기본 생성 대상 6 + 반려동물 1)
-- ----------------------------------------------------------------
INSERT INTO category_presets (preset_key, name, icon, cycle_days, note, sort_order, is_default, is_active) VALUES
                                                                                                        ('bath',    '욕실',       'bath',    14, '물때와 습기만 잡아도 관리가 쉬워져요.',              1, TRUE,  TRUE),
                                                                                                        ('kitchen', '주방',       'kitchen', 7,  '배수구와 조리대 표면을 기준으로 잡아요.',            2, TRUE,  TRUE),
                                                                                                        ('laundry', '세탁/침구',  'laundry', 14, '침구와 수건을 같은 리듬으로 관리해요.',              3, TRUE,  TRUE),
                                                                                                        ('trash',   '쓰레기/수거', 'trash',   3,  '배달이 많은 주에는 조금 짧게 잡아도 좋아요.',        4, TRUE,  TRUE),
                                                                                                        ('floor',   '바닥/먼지',  'floor',   7,  '머리카락과 먼지를 먼저 잡는 카테고리예요.',          5, TRUE,  TRUE),
                                                                                                        ('season',  '계절/가전',  'season',  28, '에어컨 필터, 제습, 결로처럼 계절에 따라 챙겨요.',    6, TRUE,  TRUE),
                                                                                                        ('pet',     '반려동물',   'floor',   7,  '털, 냄새, 패드 주변을 한 카테고리로 관리해요.',      7, FALSE, TRUE);

-- ----------------------------------------------------------------
-- 데모 사용자의 카테고리 6개
-- last_done_at을 "오늘 기준 N일 전"으로 넣어 due/soon/good이
-- 실행 시점과 무관하게 재현되도록 함
--   욕실   : cycle 14, -16일 -> 이미 지남      => due
--   주방   : cycle 7,  -6일  -> 내일 마감      => soon
--   세탁   : cycle 14, -3일  -> 11일 남음      => good
--   쓰레기 : cycle 3,  -2일  -> 내일 마감      => soon
--   바닥   : cycle 7,  -1일  -> 6일 남음       => good
--   계절   : cycle 28, -25일 -> 3일 남음       => good
-- ----------------------------------------------------------------
INSERT INTO cleaning_categories (id, user_id, preset_key, name, icon, cycle_days, last_done_at, note, sort_order, is_active) VALUES
                                                                                                                                 ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'bath',    '욕실',       'bath',    14, DATEADD('DAY', -16, CURRENT_TIMESTAMP), '물때와 습기만 잡아도 관리가 쉬워져요.',       1, TRUE),
                                                                                                                                 ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'kitchen', '주방',       'kitchen', 7,  DATEADD('DAY', -6,  CURRENT_TIMESTAMP), '배수구와 조리대 표면을 기준으로 잡아요.',     2, TRUE),
                                                                                                                                 ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'laundry', '세탁/침구',  'laundry', 14, DATEADD('DAY', -3,  CURRENT_TIMESTAMP), '침구와 수건을 같은 리듬으로 관리해요.',       3, TRUE),
                                                                                                                                 ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'trash',   '쓰레기/수거', 'trash',   3,  DATEADD('DAY', -2,  CURRENT_TIMESTAMP), '배달이 많은 주에는 조금 짧게 잡아도 좋아요.', 4, TRUE),
                                                                                                                                 ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'floor',   '바닥/먼지',  'floor',   7,  DATEADD('DAY', -1,  CURRENT_TIMESTAMP), '머리카락과 먼지를 먼저 잡는 카테고리예요.',   5, TRUE),
                                                                                                                                 ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'season',  '계절/가전',  'season',  28, DATEADD('DAY', -25, CURRENT_TIMESTAMP), '에어컨 필터, 제습, 결로처럼 계절에 따라 챙겨요.', 6, TRUE);

-- ----------------------------------------------------------------
-- 완료 기록 10건 (홈/마이 요약, weeklyFootprints 데모용으로
-- 최근 40일 범위에 분산)
-- ----------------------------------------------------------------
INSERT INTO completion_logs (user_id, category_id, category_name, completed_at) VALUES
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', '욕실',       DATEADD('DAY', -16, CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', '욕실',       DATEADD('DAY', -40, CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', '주방',       DATEADD('DAY', -6,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', '주방',       DATEADD('DAY', -13, CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', '세탁/침구',  DATEADD('DAY', -3,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', '쓰레기/수거', DATEADD('DAY', -2,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', '쓰레기/수거', DATEADD('DAY', -9,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', '바닥/먼지',  DATEADD('DAY', -1,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', '바닥/먼지',  DATEADD('DAY', -8,  CURRENT_TIMESTAMP)),
                                                                                    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', '계절/가전',  DATEADD('DAY', -25, CURRENT_TIMESTAMP));

-- ----------------------------------------------------------------
-- 셀렉션 7종
-- ----------------------------------------------------------------
INSERT INTO selection_items (id, slug, type, category, title, label, price_text, affiliate_text, reason, fit_for, notice, is_highlighted, external_url, status, sort_order) VALUES
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000001', 'starter-kit',      'kit',          '전체',        '자취 첫 달 기본 청소 키트',   '가장 먼저 담기 좋음',   '19,000원대',      '일부 제휴', '세제 종류를 늘리기보다 매주 쓰는 것만 담았어요.',                 '처음 자취를 시작했거나 청소 도구가 거의 없는 사용자', '가격은 예시이며 최종 가격은 판매 페이지에서 확인하세요.', TRUE,  NULL, 'published', 1),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000002', 'bath-soft-start',  'product',      '욕실',        '욕실 물때 입문 세트',        '처음 쓰기 쉬움',       '9,000원대',       '제휴',      '강한 세제보다 부드러운 솔과 물기 제거 도구를 먼저 쓰면 실패가 적어요.', '욕실 청소를 미루다가 한 번에 크게 하려는 사용자',       '가격대는 판매처마다 다를 수 있어요.',                      TRUE,  NULL, 'published', 2),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000003', 'sink-weekly',      'product',      '주방',        '싱크대 주간 관리 팩',        '주 1회 루틴용',        '7,000원대',       '일부 제휴', '배수구 냄새는 주 1회 관리 습관으로 대부분 잡혀요.',               '요리를 자주 하지만 배수구 관리가 밀리는 사용자',        '가격은 예시이며 구성품은 변경될 수 있어요.',               FALSE, NULL, 'published', 3),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000004', 'laundry-bedding',  'service',      '세탁/침구',   '침구 수거 서비스 비교',      '한 번에 맡기기',       '건당 15,000원대부터', '일부 제휴', '무거운 이불을 직접 세탁하기 어려운 경우 수거 서비스가 더 현실적이에요.', '세탁기 용량이 작거나 이불을 직접 세탁하기 번거로운 사용자', '가격은 예시 또는 범위이며 외부 페이지에서 최종 확인해야 합니다.', FALSE, NULL, 'published', 4),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000005', 'trash-pickup',     'subscription', '쓰레기/수거', '문앞 수거 서비스 3곳',       '반복 밀림에 추천',     '월 9,900원대부터', '제휴 포함', '쓰레기 배출이 매번 밀리는 사람에게는 반복 수거 서비스가 더 직접적인 해결책이에요.', '퇴근 시간이 늦어 배출 요일을 자주 놓치는 사용자',       '가격은 예시 또는 범위이며 외부 페이지에서 최종 확인해야 합니다.', TRUE,  NULL, 'published', 5),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000006', 'floor-easy',       'product',      '바닥/먼지',   '롤클리너와 극세사 조합',     '초보자 추천',         '12,000원대',      '일부 제휴', '물걸레 전에 마른 청소 도구를 먼저 쓰면 같은 자리를 두 번 안 닦아도 돼요.', '반려동물 털이나 먼지가 많은 집',                        '가격은 예시이며 구성품은 변경될 수 있어요.',               FALSE, NULL, 'published', 6),
                                                                                                                                                                                ('c0000000-0000-0000-0000-000000000007', 'season-aircon',    'service',      '계절/가전',   '에어컨 필터와 분해청소 기준', '계절 대비용',         '건당 60,000원대부터', '제휴',    '필터 청소만으로는 안 되는 시기가 있어 분해청소 기준을 같이 알아두면 좋아요.', '에어컨을 오래 켜두는 계절을 앞둔 사용자',               '가격은 예시 또는 범위이며 외부 페이지에서 최종 확인해야 합니다.', FALSE, NULL, 'published', 7);

-- ----------------------------------------------------------------
-- trash-pickup 셀렉션에 연결된 제공업체 3곳
-- ----------------------------------------------------------------
INSERT INTO provider_options (id, selection_item_id, name, rating_text, price_text, note, external_url, sort_order, is_active) VALUES
                                                                                                                                   ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000005', '오늘수거',    '4.8', '월 9,900원대',  '정기 수거',   NULL, 1, TRUE),
                                                                                                                                   ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000005', '우리동네수거', '4.5', '월 8,900원대',  '격주 수거 가능', NULL, 2, TRUE),
                                                                                                                                   ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000005', '클린박스',    '4.6', '월 11,000원대', '대형 폐기물 별도', NULL, 3, TRUE);

-- ----------------------------------------------------------------
-- 저장한 셀렉션 (starter-kit 저장 상태)
-- ----------------------------------------------------------------
INSERT INTO saved_selections (user_id, selection_item_id) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001');

-- ----------------------------------------------------------------
-- 커뮤니티 글 7건 (tips 4 + qa 3)
-- ----------------------------------------------------------------
INSERT INTO community_posts (id, type, title, tag, body, helpful_count, comments_count, answers_count, saved_count, status, is_recommended, created_at) VALUES
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000001', 'tips', '욕실은 세제보다 물기 제거가 먼저였어요',              '욕실', '샤워 후 스퀴지로 물기를 먼저 제거하면 물때가 확실히 줄어듭니다. 강한 세제보다 습관이 더 중요해요.', 128, 24, 0, 64, 'published', TRUE,  DATEADD('DAY', -5,  CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000002', 'tips', '음식물 쓰레기는 냄새 잡기보다 주기를 줄이는 게 답',    '수거', '탈취제보다 배출 주기를 앞당기는 편이 확실히 효과적이었어요.',                                    96,  12, 0, 40, 'published', FALSE, DATEADD('DAY', -9,  CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000003', 'tips', '물걸레 전에 롤클리너 한 번이면 두 번 일 안 해요',     '바닥', '먼지와 머리카락을 먼저 걷어내고 물걸레질하면 훨씬 수월합니다.',                                  74,  8,  0, 30, 'published', FALSE, DATEADD('DAY', -3,  CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000004', 'tips', '이불 세탁은 세탁보다 완전 건조가 핵심',               '세탁', '덜 마른 상태로 개면 냄새가 다시 생겨요. 건조를 확실히 해주세요.',                                61,  5,  0, 27, 'published', FALSE, DATEADD('DAY', -12, CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000005', 'qa',   '대리석 세면대에 물때 제거제 써도 되나요?',            '욕실', '산성 세제는 표면을 상하게 할 수 있다는 답변이 가장 많이 도움을 받았어요.',                        88,  0,  6, 22, 'published', FALSE, DATEADD('DAY', -7,  CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000006', 'qa',   '수건 냄새가 세탁 후에도 남을 때 뭘 먼저 봐야 하나요?', '세탁', '세탁조 내부 잔여물과 덜 마른 수건이 원인인 경우가 많다는 의견이 많았어요.',                       52,  0,  4, 15, 'published', FALSE, DATEADD('DAY', -15, CURRENT_TIMESTAMP)),
                                                                                                                                                            ('e0000000-0000-0000-0000-000000000007', 'qa',   '분리수거함은 몇 칸짜리가 현실적으로 좋나요?',         '수거', '3칸 이상은 오히려 안 쓰게 된다는 답변이 많았습니다.',                                            40,  0,  3, 9,  'published', FALSE, DATEADD('DAY', -20, CURRENT_TIMESTAMP));

-- ----------------------------------------------------------------
-- 데모 사용자의 반응 2건 (도움됨 1 + 저장 1)
-- ----------------------------------------------------------------
INSERT INTO community_reactions (user_id, post_id, reaction_type) VALUES
                                                                      ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'helpful'),
                                                                      ('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000005', 'save');
