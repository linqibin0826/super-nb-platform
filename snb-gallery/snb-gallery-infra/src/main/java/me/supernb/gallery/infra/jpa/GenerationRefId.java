package me.supernb.gallery.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 生成↔参考图关联复合主键 (generation_id, idx)。
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class GenerationRefId implements Serializable {

    @Column(name = "generation_id")
    private String generationId;

    @Column(name = "idx")
    private Integer idx;
}
