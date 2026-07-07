package me.supernb.gallery.infra.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/// gallery.ref_image 仓储(existsById / save 即够用)。
public interface RefImageJpaRepository extends JpaRepository<RefImageEntity, RefImageId> {
}
