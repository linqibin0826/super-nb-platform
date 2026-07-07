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

/// gallery.prompt_favorite 成员行:每人每条一次,PK 天然幂等;created_at 兼作「我的收藏」排序键。
@Entity
@Table(name = "prompt_favorite", schema = "gallery")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptFavoriteEntity {

    @EmbeddedId
    private InteractionId id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /// 新收藏成员(created_at 走审计填充)。
    public PromptFavoriteEntity(InteractionId id) {
        this.id = id;
    }
}
