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

/// 生成↔参考图引用 JPA 实体,映射 `gallery.generation_ref`。
///
/// 值对象表,继承 [ValueObjectJpaEntity]:随 generation 聚合根级联,
/// 引用 ref_image 靠 `(user_id, sha256)` 内容寻址(无外键);
/// `UNIQUE(generation_id, idx)`。
@Entity
@Table(name = "generation_ref", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationRefEntity extends ValueObjectJpaEntity {

    /// 所属生成记录。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    /// 参考图内容哈希(联合用户 id 定位 ref_image)。
    @Column(name = "sha256")
    private String sha256;

    /// 参考序号(0 起)。
    @Column(name = "idx")
    private Integer idx;

    /// 构造:新引用,雪花 id 应用层预分配;仅聚合根 `addRef` 调用。
    GenerationRefEntity(GenerationEntity generation, int idx, String sha256) {
        setId(SnowflakeIdGenerator.getId());
        this.generation = generation;
        this.idx = idx;
        this.sha256 = sha256;
    }
}
