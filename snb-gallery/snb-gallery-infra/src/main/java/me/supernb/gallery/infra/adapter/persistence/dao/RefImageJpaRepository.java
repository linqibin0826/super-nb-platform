package me.supernb.gallery.infra.adapter.persistence.dao;

import me.supernb.gallery.infra.adapter.persistence.entity.RefImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// ref_image 表 Spring Data 仓储。
public interface RefImageJpaRepository extends JpaRepository<RefImageEntity, Long> {

    /// 用户参考图库中是否已有该内容哈希(上传去重)。
    boolean existsByUserIdAndSha256(long userId, String sha256);
}
