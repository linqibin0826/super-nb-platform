-- gallery 上下文基线 schema(最终形态)。对齐旧 gallery 库,规整命名、进 gallery schema。
CREATE SCHEMA IF NOT EXISTS gallery;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 类目:三轴(scene/style/subject)
CREATE TABLE gallery.category (
    id      SERIAL PRIMARY KEY,
    slug    TEXT NOT NULL UNIQUE,
    axis    TEXT NOT NULL CHECK (axis IN ('scene', 'style', 'subject')),
    name_en TEXT NOT NULL,
    name_zh TEXT NOT NULL,
    sort    INT  NOT NULL DEFAULT 0
);

-- 提示词条目
CREATE TABLE gallery.prompt (
    id                  BIGSERIAL PRIMARY KEY,
    source              TEXT NOT NULL CHECK (source IN ('youmind', 'ff', 'ym', 'own')),
    source_id           TEXT NOT NULL UNIQUE,
    title               TEXT NOT NULL,
    description         TEXT,
    prompt_text         TEXT NOT NULL,
    lang                TEXT,
    author_name         TEXT,
    author_link         TEXT,
    source_link         TEXT,
    image_url           TEXT,
    image_w             INT,
    image_h             INT,
    category_id         INT REFERENCES gallery.category(id),
    status              TEXT NOT NULL DEFAULT 'published',
    source_published_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    like_count          INT NOT NULL DEFAULT 0,
    fav_count           INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_prompt_category  ON gallery.prompt (category_id);
CREATE INDEX idx_prompt_status_id ON gallery.prompt (status, id DESC);
CREATE INDEX idx_prompt_title_trgm ON gallery.prompt USING gin (title gin_trgm_ops);
CREATE INDEX idx_prompt_desc_trgm  ON gallery.prompt USING gin (description gin_trgm_ops);

-- 点赞 / 收藏(每人每条一次,天然幂等)
CREATE TABLE gallery.prompt_like (
    prompt_id  BIGINT NOT NULL REFERENCES gallery.prompt(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (prompt_id, user_id)
);
CREATE INDEX idx_like_user ON gallery.prompt_like (user_id, created_at DESC);

CREATE TABLE gallery.prompt_favorite (
    prompt_id  BIGINT NOT NULL REFERENCES gallery.prompt(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (prompt_id, user_id)
);
CREATE INDEX idx_fav_user ON gallery.prompt_favorite (user_id, created_at DESC);

-- 生成历史(id 为前端 task uuid 字符串;图存私有 R2,仅存键)
CREATE TABLE gallery.generation (
    id         TEXT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    prompt     TEXT NOT NULL,
    size       TEXT NOT NULL,
    n          INT  NOT NULL,
    quality    TEXT NOT NULL,
    status     TEXT NOT NULL,
    cost       DOUBLE PRECISION,
    elapsed_ms INT  NOT NULL DEFAULT 0,
    group_name TEXT,
    key_id     BIGINT,
    error      TEXT,
    thumb_key  TEXT
);
CREATE INDEX idx_generation_user ON gallery.generation (user_id, created_at DESC);

CREATE TABLE gallery.generation_image (
    id            BIGSERIAL PRIMARY KEY,
    generation_id TEXT NOT NULL REFERENCES gallery.generation(id) ON DELETE CASCADE,
    idx           INT  NOT NULL,
    r2_key        TEXT NOT NULL,
    width         INT,
    height        INT,
    bytes         INT,
    UNIQUE (generation_id, idx)
);

CREATE TABLE gallery.ref_image (
    user_id    BIGINT NOT NULL,
    sha256     TEXT   NOT NULL,
    r2_key     TEXT   NOT NULL,
    bytes      INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, sha256)
);

CREATE TABLE gallery.generation_ref (
    generation_id TEXT NOT NULL REFERENCES gallery.generation(id) ON DELETE CASCADE,
    sha256        TEXT NOT NULL,
    idx           INT  NOT NULL,
    PRIMARY KEY (generation_id, idx)
);
