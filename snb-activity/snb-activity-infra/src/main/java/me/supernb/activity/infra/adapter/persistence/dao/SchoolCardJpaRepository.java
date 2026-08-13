package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.SchoolCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 包机邀请卡仓库。
public interface SchoolCardJpaRepository extends JpaRepository<SchoolCardEntity, Long> {

    Optional<SchoolCardEntity> findByUserId(long userId);
}
