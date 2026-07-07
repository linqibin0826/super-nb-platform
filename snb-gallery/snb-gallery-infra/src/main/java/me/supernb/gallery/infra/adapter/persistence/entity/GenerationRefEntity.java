package me.supernb.gallery.infra.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// gallery.generation_ref 行实体:生成引用了哪些参考图(按位次序,sha256 指向用户去重库)。
@Entity
@Table(name = "generation_ref", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationRefEntity {

    @EmbeddedId
    private GenerationRefId id;

    @MapsId("generationId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    @Column(name = "sha256")
    private String sha256;

    GenerationRefEntity(GenerationEntity generation, int idx, String sha256) {
        this.id = new GenerationRefId(null, idx);
        this.generation = generation;
        this.sha256 = sha256;
    }
}
