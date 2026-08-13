package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.SchoolClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 开学季领取台账仓库。
public interface SchoolClaimJpaRepository extends JpaRepository<SchoolClaimEntity, Long> {

    Optional<SchoolClaimEntity> findByUserIdAndKindAndTier(long userId, String kind, int tier);

    List<SchoolClaimEntity> findByUserId(long userId);
}
