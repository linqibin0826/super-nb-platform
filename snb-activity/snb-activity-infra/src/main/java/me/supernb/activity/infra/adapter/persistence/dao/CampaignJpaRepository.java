package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// activity.campaign 仓储。
public interface CampaignJpaRepository extends JpaRepository<CampaignEntity, Long> {

    Optional<CampaignEntity> findFirstByStatusOrderByIdDesc(String status);
}
