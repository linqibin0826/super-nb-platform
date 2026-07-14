package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.AchievementDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementDefinitionJpaRepository extends JpaRepository<AchievementDefinitionEntity, Long> {

    List<AchievementDefinitionEntity> findByStatus(String status);

    Optional<AchievementDefinitionEntity> findByCode(String code);
}
