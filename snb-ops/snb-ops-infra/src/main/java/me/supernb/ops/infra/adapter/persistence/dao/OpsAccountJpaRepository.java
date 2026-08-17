package me.supernb.ops.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.ops.infra.adapter.persistence.entity.OpsAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 账号 DAO。
public interface OpsAccountJpaRepository extends JpaRepository<OpsAccountEntity, Long> {

    List<OpsAccountEntity> findAllByOrderByCreatedAtDesc();
}
