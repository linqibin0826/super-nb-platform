package me.supernb.activity.infra.adapter.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.infra.adapter.persistence.dao.AchievementDefinitionJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.AchievementDefinitionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/// AchievementCatalogPort 实现:纯读,直接把 Entity 映射成领域视图;系列标题表(自然键小型
/// 内容表,无需 JPA 实体)另走 JdbcTemplate,仿 user_metric/checkin_scan_watermark 惯例。
@Repository
public class AchievementCatalogAdapter implements AchievementCatalogPort {

    private final AchievementDefinitionJpaRepository repo;
    private final JdbcTemplate jdbc;

    public AchievementCatalogAdapter(AchievementDefinitionJpaRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @Override
    public List<AchievementDefinition> activeDefinitions() {
        return repo.findByStatus("active").stream().map(AchievementCatalogAdapter::toDomain).toList();
    }

    @Override
    public List<AchievementDefinition> allDefinitions() {
        return repo.findAll().stream().map(AchievementCatalogAdapter::toDomain).toList();
    }

    @Override
    public Optional<AchievementDefinition> byCode(String code) {
        return repo.findByCode(code).map(AchievementCatalogAdapter::toDomain);
    }

    @Override
    public Map<String, String> allSeriesLabels() {
        return jdbc.query("SELECT series_code, series_name FROM activity.achievement_series_label",
                        rs -> {
                            Map<String, String> out = new java.util.LinkedHashMap<>();
                            while (rs.next()) {
                                out.put(rs.getString("series_code"), rs.getString("series_name"));
                            }
                            return out;
                        });
    }

    private static AchievementDefinition toDomain(AchievementDefinitionEntity e) {
        return new AchievementDefinition(e.getCode(), e.getSeriesCode(), e.getTierLevel(), e.getCategory(),
                e.getRarity(), e.getNbPoints(), e.isHiddenReveal(), e.isAlwaysPrivate(), e.getStatus(),
                e.getPredicateKind(), e.getMetricCode(), e.getThresholdValue(), e.getComparator(),
                e.getPrerequisite(), e.getLaunchDate(), e.getSortOrder(), e.getName(), e.getFlavorText(),
                e.getHiddenHintText(), e.getConditionText());
    }
}
