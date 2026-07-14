package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinRewardGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 补给资格发放台账仓库。
public interface CheckinRewardGrantJpaRepository extends JpaRepository<CheckinRewardGrantEntity, Long> {

    List<CheckinRewardGrantEntity> findByStatus(String status);

    List<CheckinRewardGrantEntity> findByUserIdOrderByGrantMonthDesc(long userId);

    List<CheckinRewardGrantEntity> findByUserIdAndStatusOrderByGrantMonthDesc(long userId, String status);
}
