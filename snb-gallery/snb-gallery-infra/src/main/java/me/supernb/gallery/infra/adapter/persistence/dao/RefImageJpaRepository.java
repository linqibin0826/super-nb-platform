package me.supernb.gallery.infra.adapter.persistence.dao;

import me.supernb.gallery.infra.adapter.persistence.entity.RefImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// gallery.ref_image 仓储。
public interface RefImageJpaRepository extends JpaRepository<RefImageEntity, Long> {

    /// 用户参考图库中是否已存在该内容哈希,供上传前去重预检。
    boolean existsByUserIdAndSha256(long userId, String sha256);
}
