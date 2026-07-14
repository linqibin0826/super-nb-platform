package me.supernb.activity.app.usecase.achievement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import org.junit.jupiter.api.Test;

class AchievementContentWhitelistTest {

    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementContentWhitelist whitelist = new AchievementContentWhitelist(catalogPort);

    private static AchievementDefinition def(String category, String status, String predicateKind,
            String comparator) {
        return new AchievementDefinition("probe", null, null, category, "T1", 5, false, false, status,
                predicateKind, "probe_metric", new BigDecimal("1"), comparator, null,
                LocalDate.of(2026, 7, 13), 0, "占位", "占位文案", null, "占位条件");
    }

    @Test
    void allSeededCategoriesAndStatusesPassWhitelist() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("入职档案", "active", "metric_threshold", "gte"),
                def("机密档案", "active", "metric_threshold", "gte"),
                def("元编年史", "active", "meta_combo", null),
                def("联动矩阵", "active", "metric_threshold", "lte")));

        assertThat(whitelist.validateOnStartup()).isEqualTo(4); // 返回校验通过的行数,供启动日志打印
    }

    @Test
    void unknownCategoryFailsFast() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("不存在的类目", "active", "metric_threshold", "gte")));

        assertThatThrownBy(whitelist::validateOnStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("probe")
                .hasMessageContaining("不存在的类目");
    }

    @Test
    void unknownStatusFailsFast() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("入职档案", "published", "metric_threshold", "gte")));

        assertThatThrownBy(whitelist::validateOnStartup).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownPredicateKindFailsFast() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("入职档案", "active", "javascript_eval", "gte")));

        assertThatThrownBy(whitelist::validateOnStartup).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownComparatorFailsFast() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("入职档案", "active", "metric_threshold", "eq")));

        assertThatThrownBy(whitelist::validateOnStartup).isInstanceOf(IllegalStateException.class);
    }
}
