package me.supernb.gallery.infra.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// gallery.prompt 行实体:提示词条目。条目由收录管线 SQL 写入,JPA 侧只读 + 计数列增减。
@Entity
@Table(name = "prompt", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source")
    private String source;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "prompt_text")
    private String promptText;

    @Column(name = "lang")
    private String lang;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_link")
    private String authorLink;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_w")
    private Integer imageW;

    @Column(name = "image_h")
    private Integer imageH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "status")
    private String status;

    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "like_count")
    private int likeCount;

    @Column(name = "fav_count")
    private int favCount;
}
