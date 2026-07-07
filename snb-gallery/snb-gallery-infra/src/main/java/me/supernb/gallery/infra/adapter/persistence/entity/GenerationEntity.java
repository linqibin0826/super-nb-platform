package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
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
/// 聚合根,继承 [BaseJpaEntity];**雪花 id 是唯一身份,对内对外同一条(验收意见⑦)**,
/// 由仓储 `nextId()` 预分配后传入构造器——R2 对象键(`gen/{userId}/{id}/…`)必须
/// 先于持久化确定;created_by 由审计自动填充,即发起本次生成的用户。
@Entity
@Table(name = "generation", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationEntity extends BaseJpaEntity {

    /// 发起生成的用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 本次生成使用的提示词。
    @Column(name = "prompt")
    private String prompt;

    /// 输出尺寸档,如 `1024x1024`。
    @Column(name = "size")
    private String size;

    /// 本次生成的出图张数。
    @Column(name = "n")
    private Integer n;

    /// 输出画质档。
    @Column(name = "quality")
    private String quality;

    /// 生成任务终态:`done` | `error`。
    @Column(name = "status")
    private String status;

    /// 本次生成消耗的额度(USD 名义计价)。
    @Column(name = "cost")
    private Double cost;

    /// 本次生成耗时,单位毫秒。
    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    /// 计费所属分组名。
    @Column(name = "group_name")
    private String groupName;

    /// 本次调用使用的 API Key id。
    @Column(name = "key_id")
    private Long keyId;

    /// 失败原因,成功时为 NULL。
    @Column(name = "error")
    private String error;

    /// 列表用 256px 缩略图的 R2 键;生成失败时可为 NULL,此时列表回退展示首图。
    @Column(name = "thumb_key")
    private String thumbKey;

    /// 本次生成的输出图集合,随聚合根级联。
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx")
    private List<GenerationImageEntity> images = new ArrayList<>();

    /// 本次生成引用的参考图集合,随聚合根级联。
    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx")
    private List<GenerationRefEntity> refs = new ArrayList<>();

    /// 构造:新建生成记录;id 取仓储 `nextId()` 预分配的雪花值(R2 对象键已按该 id 命名)。
    public GenerationEntity(long id, long userId, String prompt, String size, int n, String quality,
                            String status, Double cost, int elapsedMs, String groupName, Long keyId,
                            String error, String thumbKey) {
        setId(id);
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

    /// 追加一张输出图,级联随聚合根持久化。
    public void addImage(int idx, String r2Key, Integer bytes) {
        images.add(new GenerationImageEntity(this, idx, r2Key, bytes));
    }

    /// 追加一条参考图引用,级联随聚合根持久化。
    public void addRef(int idx, String sha256) {
        refs.add(new GenerationRefEntity(this, idx, sha256));
    }
}
