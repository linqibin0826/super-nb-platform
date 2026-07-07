package me.supernb.activity.infra.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/// activity.campaign 仓储。
public interface CampaignJpaRepository extends JpaRepository<CampaignEntity, Long> {

    Optional<CampaignEntity> findFirstByStatusOrderByIdDesc(String status);
}
