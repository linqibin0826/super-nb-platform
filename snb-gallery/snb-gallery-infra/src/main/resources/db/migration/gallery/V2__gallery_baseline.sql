-- gallery 上下文基线 schema(最终形态)。对齐旧 gallery 库,规整命名、进 gallery schema。
-- 审计基座(patra):id 一律应用层预分配雪花 BIGINT,无数据库自增;
-- created_at/updated_at/version 带 DEFAULT 兜底纯 SQL 写入(数据迁移/收录管线),JPA 路径由审计自动填充。
-- 值对象表(generation_image/generation_ref)只有 id,生命周期完全随聚合根。
CREATE SCHEMA IF NOT EXISTS gallery;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 类目:三轴(scene/style/subject)(聚合根,BaseJpaEntity)
CREATE TABLE gallery.category (
    id      BIGINT PRIMARY KEY,
    slug    TEXT NOT NULL UNIQUE,
    axis    TEXT NOT NULL CHECK (axis IN ('scene', 'style', 'subject')),
    name_en TEXT NOT NULL,
    name_zh TEXT NOT NULL,
    sort    INT  NOT NULL DEFAULT 0,
    record_remarks     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         BIGINT,
    created_by_name    TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    updated_by_name    TEXT,
    version            BIGINT NOT NULL DEFAULT 0,
    ip_address         BYTEA
);

-- 提示词条目(聚合根,BaseJpaEntity;收录管线纯 SQL 写入靠列 DEFAULT)
CREATE TABLE gallery.prompt (
    id                  BIGINT PRIMARY KEY,
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
    category_id         BIGINT REFERENCES gallery.category(id),
    status              TEXT NOT NULL DEFAULT 'published',
    source_published_at TIMESTAMPTZ,
    like_count          INT NOT NULL DEFAULT 0,
    fav_count           INT NOT NULL DEFAULT 0,
    record_remarks     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         BIGINT,
    created_by_name    TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    updated_by_name    TEXT,
    version            BIGINT NOT NULL DEFAULT 0,
    ip_address         BYTEA
);
CREATE INDEX idx_prompt_category  ON gallery.prompt (category_id);
CREATE INDEX idx_prompt_status_id ON gallery.prompt (status, id DESC);
CREATE INDEX idx_prompt_title_trgm ON gallery.prompt USING gin (title gin_trgm_ops);
CREATE INDEX idx_prompt_desc_trgm  ON gallery.prompt USING gin (description gin_trgm_ops);

-- 点赞 / 收藏(子实体,ChildJpaEntity;成员唯一约束保幂等,created_at 即点赞/收藏时刻)
CREATE TABLE gallery.prompt_like (
    id         BIGINT PRIMARY KEY,
    prompt_id  BIGINT NOT NULL REFERENCES gallery.prompt(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_like_prompt_user UNIQUE (prompt_id, user_id)
);
CREATE INDEX idx_like_user ON gallery.prompt_like (user_id, created_at DESC);

CREATE TABLE gallery.prompt_favorite (
    id         BIGINT PRIMARY KEY,
    prompt_id  BIGINT NOT NULL REFERENCES gallery.prompt(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fav_prompt_user UNIQUE (prompt_id, user_id)
);
CREATE INDEX idx_fav_user ON gallery.prompt_favorite (user_id, created_at DESC);

-- 生成历史(聚合根,BaseJpaEntity;雪花 id 即唯一身份,对外 JSON 以字符串输出)
CREATE TABLE gallery.generation (
    id             BIGINT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    prompt         TEXT NOT NULL,
    size           TEXT NOT NULL,
    n              INT  NOT NULL,
    quality        TEXT NOT NULL,
    status         TEXT NOT NULL,
    cost           DOUBLE PRECISION,
    elapsed_ms     INT  NOT NULL DEFAULT 0,
    group_name     TEXT,
    key_id         BIGINT,
    error          TEXT,
    thumb_key      TEXT,
    record_remarks     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         BIGINT,
    created_by_name    TEXT,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by         BIGINT,
    updated_by_name    TEXT,
    version            BIGINT NOT NULL DEFAULT 0,
    ip_address         BYTEA
);
CREATE INDEX idx_generation_user ON gallery.generation (user_id, created_at DESC);

-- 输出图(值对象表,ValueObjectJpaEntity:随聚合根整体管理)
CREATE TABLE gallery.generation_image (
    id            BIGINT PRIMARY KEY,
    generation_id BIGINT NOT NULL REFERENCES gallery.generation(id) ON DELETE CASCADE,
    idx           INT  NOT NULL,
    r2_key        TEXT NOT NULL,
    width         INT,
    height        INT,
    bytes         INT,
    CONSTRAINT uk_genimg_generation_idx UNIQUE (generation_id, idx)
);

-- 参考图库(子实体,ChildJpaEntity;按用户 sha256 内容寻址去重)
CREATE TABLE gallery.ref_image (
    id         BIGINT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    sha256     TEXT   NOT NULL,
    r2_key     TEXT   NOT NULL,
    bytes      INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ref_user_sha UNIQUE (user_id, sha256)
);

-- 生成↔参考图引用(值对象表,ValueObjectJpaEntity)
CREATE TABLE gallery.generation_ref (
    id            BIGINT PRIMARY KEY,
    generation_id BIGINT NOT NULL REFERENCES gallery.generation(id) ON DELETE CASCADE,
    sha256        TEXT NOT NULL,
    idx           INT  NOT NULL,
    CONSTRAINT uk_genref_generation_idx UNIQUE (generation_id, idx)
);
