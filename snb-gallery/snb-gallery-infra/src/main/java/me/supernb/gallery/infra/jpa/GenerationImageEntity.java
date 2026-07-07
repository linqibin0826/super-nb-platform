package me.supernb.gallery.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// gallery.generation_image 行实体:生成的一张输出图(仅存 R2 键)。
@Entity
@Table(name = "generation_image", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    @Column(name = "idx")
    private Integer idx;

    @Column(name = "r2_key")
    private String r2Key;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "bytes")
    private Integer bytes;

    GenerationImageEntity(GenerationEntity generation, int idx, String r2Key, Integer bytes) {
        this.generation = generation;
        this.idx = idx;
        this.r2Key = r2Key;
        this.bytes = bytes;
    }
}
