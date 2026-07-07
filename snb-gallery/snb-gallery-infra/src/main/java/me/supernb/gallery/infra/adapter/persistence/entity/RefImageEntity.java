package me.supernb.gallery.infra.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/// gallery.ref_image 行实体:用户参考图去重库(sha256 按用户唯一,仅存 R2 键)。
@Entity
@Table(name = "ref_image", schema = "gallery")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefImageEntity {

    @EmbeddedId
    private RefImageId id;

    @Column(name = "r2_key")
    private String r2Key;

    @Column(name = "bytes")
    private Integer bytes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /// 新参考图记录((user,sha) 内容寻址)。
    public RefImageEntity(RefImageId id, String r2Key, Integer bytes) {
        this.id = id;
        this.r2Key = r2Key;
        this.bytes = bytes;
    }
}
