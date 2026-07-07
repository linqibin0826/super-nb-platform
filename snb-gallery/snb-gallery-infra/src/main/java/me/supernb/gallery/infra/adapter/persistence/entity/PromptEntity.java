package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 提示词条目 JPA 实体,映射 `gallery.prompt`。
///
/// 聚合根,继承 [BaseJpaEntity];条目数据由收录管线以纯 SQL 直接写入(审计列靠
/// 列 DEFAULT 兜底),应用侧只通过 `@Modifying` 批量语句改动计数列,不经过乐观锁。
@Entity
@Table(name = "prompt", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptEntity extends BaseJpaEntity {

    /// 数据来源渠道:`youmind` | `ff` | `ym` | `own`。
    @Column(name = "source")
    private String source;

    /// 来源侧唯一标识,作为收录去重的幂等键。
    @Column(name = "source_id")
    private String sourceId;

    /// 条目标题。
    @Column(name = "title")
    private String title;

    /// 条目简介。
    @Column(name = "description")
    private String description;

    /// 提示词正文文本。
    @Column(name = "prompt_text")
    private String promptText;

    /// 内容语种。
    @Column(name = "lang")
    private String lang;

    /// 创作者署名。
    @Column(name = "author_name")
    private String authorName;

    /// 创作者主页链接。
    @Column(name = "author_link")
    private String authorLink;

    /// 来源页面链接。
    @Column(name = "source_link")
    private String sourceLink;

    /// 预览图 CDN 完整地址。
    @Column(name = "image_url")
    private String imageUrl;

    /// 预览图宽度,单位 px。
    @Column(name = "image_w")
    private Integer imageW;

    /// 预览图高度,单位 px。
    @Column(name = "image_h")
    private Integer imageH;

    /// 所属类目,可为空。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    /// 状态,仅 `published` 对外可见。
    @Column(name = "status")
    private String status;

    /// 来源侧发布时间。
    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;

    /// 点赞数,反规范化冗余列。
    @Column(name = "like_count")
    private int likeCount;

    /// 收藏数,反规范化冗余列。
    @Column(name = "fav_count")
    private int favCount;
}
