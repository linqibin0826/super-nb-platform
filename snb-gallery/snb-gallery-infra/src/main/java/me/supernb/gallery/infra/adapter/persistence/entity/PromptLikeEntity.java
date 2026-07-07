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

/// gallery.prompt_like 成员行:每人每条一次,PK 天然幂等。
@Entity
@Table(name = "prompt_like", schema = "gallery")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptLikeEntity {

    @EmbeddedId
    private InteractionId id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public PromptLikeEntity(InteractionId id) {
        this.id = id;
    }
}
