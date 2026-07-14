package me.supernb.activity.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.infra.adapter.persistence.dao.AchievementDefinitionJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.AchievementDefinitionEntity;
import org.springframework.stereotype.Repository;

/// AchievementCatalogPort 实现:纯读,直接把 Entity 映射成领域视图。
@Repository
public class AchievementCatalogAdapter implements AchievementCatalogPort {

    private final AchievementDefinitionJpaRepository repo;

    public AchievementCatalogAdapter(AchievementDefinitionJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<AchievementDefinition> activeDefinitions() {
        return repo.findByStatus("active").stream().map(AchievementCatalogAdapter::toDomain).toList();
    }

    @Override
    public Optional<AchievementDefinition> byCode(String code) {
        return repo.findByCode(code).map(AchievementCatalogAdapter::toDomain);
    }

    private static AchievementDefinition toDomain(AchievementDefinitionEntity e) {
        return new AchievementDefinition(e.getCode(), e.getSeriesCode(), e.getTierLevel(), e.getCategory(),
                e.getRarity(), e.getNbPoints(), e.isHiddenReveal(), e.isAlwaysPrivate(), e.getStatus(),
                e.getPredicateKind(), e.getMetricCode(), e.getThresholdValue(), e.getComparator(),
                e.getPrerequisite(), e.getLaunchDate(), e.getSortOrder(), e.getName(), e.getFlavorText(),
                e.getHiddenHintText(), e.getConditionText());
    }
}
