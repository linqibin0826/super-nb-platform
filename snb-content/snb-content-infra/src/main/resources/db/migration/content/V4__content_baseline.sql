-- content 上下文基线：分类 + 文章（内容中心 hub.super-nb.me，设计稿 ai-relay specs/2026-07-10-hub-content-center-design.md）
-- 审计列照 gallery.prompt 模板（patra 审计基座）；id 为应用层雪花，无数据库自增。

CREATE TABLE content.category (
    slug        TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    sort_order  INT  NOT NULL DEFAULT 0
);

CREATE TABLE content.article (
    id              BIGINT PRIMARY KEY,
    slug            TEXT NOT NULL UNIQUE,
    type            TEXT NOT NULL CHECK (type IN ('article', 'ebook')),
    title           TEXT NOT NULL,
    summary         TEXT NOT NULL DEFAULT '',
    cover_url       TEXT,
    category_slug   TEXT NOT NULL REFERENCES content.category(slug),
    tags            JSONB NOT NULL DEFAULT '[]'::jsonb,
    body_html       TEXT,
    ebook_path      TEXT,
    source_name     TEXT,
    source_url      TEXT,
    published_at    TIMESTAMPTZ NOT NULL,
    hidden          BOOLEAN NOT NULL DEFAULT FALSE,
    record_remarks  JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    created_by_name TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT,
    updated_by_name TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    ip_address      BYTEA
);

-- 列表主序（可见 + 倒序）/ 分类过滤 / 标签 jsonb 包含
CREATE INDEX idx_content_article_visible  ON content.article (hidden, published_at DESC);
CREATE INDEX idx_content_article_category ON content.article (category_slug, published_at DESC);
CREATE INDEX idx_content_article_tags     ON content.article USING GIN (tags);
