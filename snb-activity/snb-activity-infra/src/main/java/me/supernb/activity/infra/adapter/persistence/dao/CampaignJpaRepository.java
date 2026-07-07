package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// `activity.campaign` 仓储。
public interface CampaignJpaRepository extends JpaRepository<CampaignEntity, Long> {

    /// 按 status 取最新一期(id 降序取首条);无匹配返回 empty。
    Optional<CampaignEntity> findFirstByStatusOrderByIdDesc(String status);
}
