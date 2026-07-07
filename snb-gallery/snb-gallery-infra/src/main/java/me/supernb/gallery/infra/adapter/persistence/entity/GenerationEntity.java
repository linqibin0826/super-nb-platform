package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 生成历史 JPA 实体,映射 `gallery.generation`。
///
/// 聚合根,继承 [BaseJpaEntity];雪花 id 仅为内部代理键,
/// **对外与幂等标识是 `client_task_id`(前端任务 uuid,全局唯一)**——
/// domain/app/API 说的 `id` 都指它,涟漪锁死在 infra 层。
/// created_by 由审计自动填=生成发起用户。
@Entity
@Table(name = "generation", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationEntity extends BaseJpaEntity {

    /// 前端任务 uuid(对外标识 + 建单幂等键,全局唯一)。
    @Column(name = "client_task_id", updatable = false)
    private String clientTaskId;

    /// 生成发起用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 生成提示词。
    @Column(name = "prompt")
    private String prompt;

    /// 尺寸档(如 `1024x1024`)。
    @Column(name = "size")
    private String size;

    /// 出图张数。
    @Column(name = "n")
    private Integer n;

    /// 画质档。
    @Column(name = "quality")
    private String quality;

    /// 任务终态:`done` | `error`。
    @Column(name = "status")
    private String status;

    /// 本次消耗额度(USD 名义计价)。
    @Column(name = "cost")
    private Double cost;

    /// 生成耗时毫秒。
    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    /// 计费分组名。
    @Column(name = "group_name")
    private String groupName;

    /// 使用的 API Key id。
    @Column(name = "key_id")
    private Long keyId;

    /// 失败原因(成功为 NULL)。
    @Column(name = "error")
    private String error;

    /// 256px 列表缩略图 R2 键(生成失败时可为 NULL,列表回退首图)。
    @Column(name = "thumb_key")
    private String thumbKey;

    /// 输出图集合(级联随聚合根)。
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx")
    private List<GenerationImageEntity> images = new ArrayList<>();

    /// 参考图引用集合(级联随聚合根)。
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx")
    private List<GenerationRefEntity> refs = new ArrayList<>();

    /// 构造:新生成记录,雪花 id 应用层预分配;`clientTaskId` 为前端任务 uuid。
    public GenerationEntity(String clientTaskId, long userId, String prompt, String size, int n, String quality,
                            String status, Double cost, int elapsedMs, String groupName, Long keyId,
                            String error, String thumbKey) {
        setId(SnowflakeIdGenerator.getId());
        this.clientTaskId = clientTaskId;
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

    /// 追加一张输出图(级联持久化)。
    public void addImage(int idx, String r2Key, Integer bytes) {
        images.add(new GenerationImageEntity(this, idx, r2Key, bytes));
    }

    /// 追加一条参考图引用(级联持久化)。
    public void addRef(int idx, String sha256) {
        refs.add(new GenerationRefEntity(this, idx, sha256));
    }
}
