-- ================================================================
-- 클린루프 (CleanLoop) H2 스키마
-- 원본: docs/issues/schema.sql 의 DDL 구간
-- ================================================================

-- 재실행 대비 정리 (의존성 역순)
DROP TABLE IF EXISTS community_reactions;
DROP TABLE IF EXISTS community_posts;
DROP TABLE IF EXISTS saved_selections;
DROP TABLE IF EXISTS provider_options;
DROP TABLE IF EXISTS selection_items;
DROP TABLE IF EXISTS completion_logs;
DROP TABLE IF EXISTS cleaning_categories;
DROP TABLE IF EXISTS category_presets;
DROP TABLE IF EXISTS users;

-- 01-user.md
CREATE TABLE users (
    id           UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name         VARCHAR(40) NOT NULL,
    avatar_text  VARCHAR(4),
    timezone     VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 02-category.md
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
-- 같은 사용자 내 활성 이름/프리셋 중복 방지는 Service 레벨에서 검증한다.
CREATE INDEX idx_categories_user_active_sort ON cleaning_categories(user_id, is_active, sort_order);
CREATE INDEX idx_categories_user_preset ON cleaning_categories(user_id, preset_key);

-- 03-completion.md
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

-- 05-selection.md
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

-- 06-community.md
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
