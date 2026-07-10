package me.supernb.content.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.content.domain.port.repository.ArticleRepository;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/// 文章 JPA 实体，映射 `content.article`。聚合根，继承 [BaseJpaEntity]（全套审计列）。
///
/// `slug` 唯一约束是发布幂等键；`tags` 为 jsonb 字符串数组（GIN 索引支撑标签过滤）。
@Entity
@Table(name = "article", schema = "content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity extends BaseJpaEntity {

    /// 发布幂等键 + 前端路由键，全表唯一。
    @Column(nullable = false, unique = true)
    private String slug;

    /// article / ebook（管线按内容仓库目录判定）。
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    /// 封面 CDN URL，可空（空走纯文字卡）。
    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "category_slug", nullable = false)
    private String categorySlug;

    /// 自由标签，jsonb 字符串数组。
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> tags;

    /// 管线预渲染正文（ebook 为 null）。
    @Column(name = "body_html")
    private String bodyHtml;

    /// ebook 专用站内路径 `books/<slug>.html`（article 为 null）。
    @Column(name = "ebook_path")
    private String ebookPath;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_url")
    private String sourceUrl;

    /// 列表排序键。
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    /// 下架软开关：列表消失、详情 404，数据保留（二期互动表 FK 依赖）。
    @Column(nullable = false)
    private boolean hidden;

    /// 新建：雪花取号 + 全量赋值（slug 合法性由发布管线与 app 层守卫）。
    public ArticleEntity(ArticleRepository.ArticleData data) {
        setId(SnowflakeIdGenerator.getId());
        apply(data);
    }

    /// 改稿重发：slug 幂等命中后全量覆盖（id 不变，审计列由基座维护）。
    public void apply(ArticleRepository.ArticleData data) {
        this.slug = data.slug();
        this.type = data.type();
        this.title = data.title();
        this.summary = data.summary();
        this.coverUrl = data.coverUrl();
        this.categorySlug = data.categorySlug();
        this.tags = List.copyOf(data.tags());
        this.bodyHtml = data.bodyHtml();
        this.ebookPath = data.ebookPath();
        this.sourceName = data.sourceName();
        this.sourceUrl = data.sourceUrl();
        this.publishedAt = data.publishedAt();
        this.hidden = data.hidden();
    }
}
