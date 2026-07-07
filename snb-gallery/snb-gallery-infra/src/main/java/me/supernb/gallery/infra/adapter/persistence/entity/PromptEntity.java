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
/// 聚合根,继承 [BaseJpaEntity];条目由收录管线纯 SQL 写入(靠列 DEFAULT 补审计值),
/// 应用侧只调计数列(`@Modifying` 批量语句,不经乐观锁)。
@Entity
@Table(name = "prompt", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptEntity extends BaseJpaEntity {

    /// 数据来源:`youmind` | `ff` | `ym` | `own`。
    @Column(name = "source")
    private String source;

    /// 来源侧唯一标识(收录幂等键)。
    @Column(name = "source_id")
    private String sourceId;

    /// 标题。
    @Column(name = "title")
    private String title;

    /// 描述。
    @Column(name = "description")
    private String description;

    /// 提示词正文。
    @Column(name = "prompt_text")
    private String promptText;

    /// 语种。
    @Column(name = "lang")
    private String lang;

    /// 作者名。
    @Column(name = "author_name")
    private String authorName;

    /// 作者主页链接。
    @Column(name = "author_link")
    private String authorLink;

    /// 来源页链接。
    @Column(name = "source_link")
    private String sourceLink;

    /// 预览图 CDN 完整 URL。
    @Column(name = "image_url")
    private String imageUrl;

    /// 预览图宽(px)。
    @Column(name = "image_w")
    private Integer imageW;

    /// 预览图高(px)。
    @Column(name = "image_h")
    private Integer imageH;

    /// 所属类目(可空)。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    /// 状态,`published` 才对外可见。
    @Column(name = "status")
    private String status;

    /// 来源侧发布时间。
    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;

    /// 点赞计数(反规范化)。
    @Column(name = "like_count")
    private int likeCount;

    /// 收藏计数(反规范化)。
    @Column(name = "fav_count")
    private int favCount;
}
