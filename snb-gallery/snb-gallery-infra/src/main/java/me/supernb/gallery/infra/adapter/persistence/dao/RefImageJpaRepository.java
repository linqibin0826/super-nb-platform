package me.supernb.gallery.infra.adapter.persistence.dao;

import me.supernb.gallery.infra.adapter.persistence.entity.RefImageEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.RefImageId;
import org.springframework.data.jpa.repository.JpaRepository;

/// gallery.ref_image 仓储(existsById / save 即够用)。
public interface RefImageJpaRepository extends JpaRepository<RefImageEntity, RefImageId> {
}
