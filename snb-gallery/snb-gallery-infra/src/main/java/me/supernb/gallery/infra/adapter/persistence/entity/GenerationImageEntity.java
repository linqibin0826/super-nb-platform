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
/// 值对象表,继承 [ValueObjectJpaEntity]:生命周期完全依附 generation 聚合根
/// (随其级联持久化/删除),不支持独立更新,不带审计列;`UNIQUE(generation_id, idx)`。
@Entity
@Table(name = "generation_image", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationImageEntity extends ValueObjectJpaEntity {

    /// 所属的生成记录。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    /// 在输出图集合中的序号,0 起。
    @Column(name = "idx")
    private Integer idx;

    /// 输出图片的 R2 对象键。
    @Column(name = "r2_key")
    private String r2Key;

    /// 图片宽度,单位 px。
    @Column(name = "width")
    private Integer width;

    /// 图片高度,单位 px。
    @Column(name = "height")
    private Integer height;

    /// 图片字节数。
    @Column(name = "bytes")
    private Integer bytes;

    /// 构造:新建输出图,雪花 id 由应用层预分配;仅供聚合根的 `addImage` 调用。
    GenerationImageEntity(GenerationEntity generation, int idx, String r2Key, Integer bytes) {
        setId(SnowflakeIdGenerator.getId());
        this.generation = generation;
        this.idx = idx;
        this.r2Key = r2Key;
        this.bytes = bytes;
    }
}
