package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 参考图库 JPA 实体,映射 `gallery.ref_image`。
///
/// 用户参考图库的子实体,继承 [ChildJpaEntity];按 `UNIQUE(user_id, sha256)`
/// 内容寻址去重,跨生成记录复用,created_at 由审计填充。
@Entity
@Table(name = "ref_image", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefImageEntity extends ChildJpaEntity {

    /// 归属用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 图片内容 sha256(与 user_id 联合唯一)。
    @Column(name = "sha256")
    private String sha256;

    /// 图片 R2 对象键。
    @Column(name = "r2_key")
    private String r2Key;

    /// 字节数。
    @Column(name = "bytes")
    private Integer bytes;

    /// 构造:新参考图,雪花 id 应用层预分配。
    public RefImageEntity(long userId, String sha256, String r2Key, Integer bytes) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.sha256 = sha256;
        this.r2Key = r2Key;
        this.bytes = bytes;
    }
}
