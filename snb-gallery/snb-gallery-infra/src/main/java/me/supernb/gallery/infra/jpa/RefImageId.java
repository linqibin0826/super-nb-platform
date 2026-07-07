package me.supernb.gallery.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 参考图去重库复合主键 (user_id, sha256):同用户同内容只存一份。
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class RefImageId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sha256")
    private String sha256;
}
