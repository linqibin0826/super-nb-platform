package me.supernb.gallery.infra.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/// gallery.generation 聚合根:一次生成任务(id 为前端 task uuid 字符串;图存私有 R2,仅存键)。
/// 输出图与参考图关联随聚合级联落库/删除。
@Entity
@Table(name = "generation", schema = "gallery")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationEntity {

    @Id
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "prompt")
    private String prompt;

    @Column(name = "size")
    private String size;

    @Column(name = "n")
    private Integer n;

    @Column(name = "quality")
    private String quality;

    @Column(name = "status")
    private String status;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "key_id")
    private Long keyId;

    @Column(name = "error")
    private String error;

    @Column(name = "thumb_key")
    private String thumbKey;

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx")
    private List<GenerationImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.idx")
    private List<GenerationRefEntity> refs = new ArrayList<>();

    public GenerationEntity(String id, long userId, String prompt, String size, int n, String quality,
                            String status, Double cost, int elapsedMs, String groupName, Long keyId,
                            String error, String thumbKey) {
        this.id = id;
        this.userId = userId;
        this.prompt = prompt;
        this.size = size;
        this.n = n;
        this.quality = quality;
        this.status = status;
        this.cost = cost;
        this.elapsedMs = elapsedMs;
        this.groupName = groupName;
        this.keyId = keyId;
        this.error = error;
        this.thumbKey = thumbKey;
    }

    public void addImage(int idx, String r2Key, Integer bytes) {
        images.add(new GenerationImageEntity(this, idx, r2Key, bytes));
    }

    public void addRef(int idx, String sha256) {
        refs.add(new GenerationRefEntity(this, idx, sha256));
    }
}
