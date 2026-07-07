package me.supernb.gallery.infra.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 点赞/收藏成员复合主键 (prompt_id, user_id)。
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class InteractionId implements Serializable {

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "user_id")
    private Long userId;
}
