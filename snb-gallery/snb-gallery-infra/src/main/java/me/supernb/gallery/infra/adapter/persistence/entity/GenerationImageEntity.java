package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ValueObjectJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 输出图 JPA 实体,映射 `gallery.generation_image`。
///
/// 值对象表,继承 [ValueObjectJpaEntity]:生命周期完全随 generation 聚合根
/// (级联持久化/删除),无独立更新,无审计列;`UNIQUE(generation_id, idx)`。
@Entity
@Table(name = "generation_image", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationImageEntity extends ValueObjectJpaEntity {

    /// 所属生成记录。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    /// 输出序号(0 起)。
    @Column(name = "idx")
    private Integer idx;

    /// 图片 R2 对象键。
    @Column(name = "r2_key")
    private String r2Key;

    /// 宽(px)。
    @Column(name = "width")
    private Integer width;

    /// 高(px)。
    @Column(name = "height")
    private Integer height;

    /// 字节数。
    @Column(name = "bytes")
    private Integer bytes;

    /// 构造:新输出图,雪花 id 应用层预分配;仅聚合根 `addImage` 调用。
    GenerationImageEntity(GenerationEntity generation, int idx, String r2Key, Integer bytes) {
        setId(SnowflakeIdGenerator.getId());
        this.generation = generation;
        this.idx = idx;
        this.r2Key = r2Key;
        this.bytes = bytes;
    }
}
