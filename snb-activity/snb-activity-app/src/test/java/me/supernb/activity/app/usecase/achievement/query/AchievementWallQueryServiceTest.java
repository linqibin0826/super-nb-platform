package me.supernb.activity.app.usecase.achievement.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.achievement.AchievementCategoryView;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.model.achievement.AchievementItemView;
import me.supernb.activity.domain.model.achievement.AchievementUnlock;
import me.supernb.activity.domain.model.achievement.AchievementWallView;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import org.junit.jupiter.api.Test;

class AchievementWallQueryServiceTest {

    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);
    private final AchievementWallQueryService service =
            new AchievementWallQueryService(catalogPort, unlockPort, metricPort);

    private static AchievementDefinition def(String code, String seriesCode, Integer tierLevel, String category,
            String rarity, int nb, boolean hiddenReveal, String predicateKind, String metricCode,
            BigDecimal threshold, String comparator, int sortOrder, String name, String condition,
            String flavorText, String hint) {
        return new AchievementDefinition(code, seriesCode, tierLevel, category, rarity, nb, hiddenReveal, false,
                "active", predicateKind, metricCode, threshold, comparator, null,
                LocalDate.of(2026, 7, 13), sortOrder, name, flavorText, hint, condition);
    }

    @Test
    void buildsRegularCategoryWithPlainItemAndSeriesProgress() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("checkin_first", null, null, "入职档案", "T1", 5, false,
                        "metric_threshold", "checkin_total_count", new BigDecimal("1"), "gte",
                        1, "开机自检", "首次签到", "每天上班第一件事:证明我还活着。", null),
                def("api_calls_1", "api_calls", 1, "机房作业", "T1", 5, false,
                        "metric_threshold", "api_call_total_count", new BigDecimal("100"), "gte",
                        10, "破壳", "累计调用满 100 次", "第 100 次调用,壳裂了,人还在。", null),
                def("api_calls_2", "api_calls", 2, "机房作业", "T2", 15, false,
                        "metric_threshold", "api_call_total_count", new BigDecimal("1000"), "gte",
                        11, "满负荷", "满 1,000 次", "负载曲线终于有了个像样的形状。", null),
                def("api_calls_3", "api_calls", 3, "机房作业", "T3", 40, false,
                        "metric_threshold", "api_call_total_count", new BigDecimal("10000"), "gte",
                        12, "重型机", "满 10,000 次", "调用量破万,风扇开始有意见。", null)));
        when(catalogPort.allSeriesLabels()).thenReturn(Map.of("api_calls", "调用量系列 · API CALLS"));
        when(unlockPort.myUnlocks(42)).thenReturn(List.of(
                new AchievementUnlock(42, "checkin_first", Instant.now(), 5, "batch_scan", true),
                new AchievementUnlock(42, "api_calls_1", Instant.now(), 5, "batch_scan", true),
                new AchievementUnlock(42, "api_calls_2", Instant.now(), 15, "batch_scan", true)));
        when(metricPort.allMetrics(42)).thenReturn(Map.of("api_call_total_count", 6842.0));

        AchievementWallView wall = service.wall(42);

        assertThat(wall.summary().unlockedCount()).isEqualTo(3);
        assertThat(wall.summary().totalCount()).isEqualTo(4);
        assertThat(wall.summary().nbTotal()).isEqualTo(25);

        AchievementCategoryView onboarding = wall.categories().get(0);
        assertThat(onboarding.name()).isEqualTo("入职档案");
        assertThat(onboarding.hidden()).isFalse();
        assertThat(onboarding.items()).hasSize(1);
        assertThat(onboarding.items().get(0).condition()).isEqualTo("首次签到");
        assertThat(onboarding.items().get(0).tier()).isEqualTo(1);

        AchievementCategoryView roomWork = wall.categories().get(1);
        assertThat(roomWork.name()).isEqualTo("机房作业");
        assertThat(roomWork.items()).isEmpty();
        assertThat(roomWork.series()).hasSize(1);
        assertThat(roomWork.series().get(0).seriesName()).isEqualTo("调用量系列 · API CALLS");
        assertThat(roomWork.series().get(0).progress().text()).isEqualTo("6,842 / 10,000");
        assertThat(roomWork.series().get(0).progress().pct()).isEqualTo(68);
        assertThat(roomWork.series().get(0).steps()).hasSize(3);
        assertThat(roomWork.series().get(0).steps().get(2).unlocked()).isFalse();
    }

    @Test
    void hiddenCategorySealsNameAndConditionUntilUnlocked() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("midnight_courier", null, null, "机密档案", "T1", 3, true,
                        "metric_threshold", "checkin_midnight_flag", new BigDecimal("1"), "gte",
                        60, "零点信使", "0 点整签到", "零点整准时打卡——闹钟辛苦了。", "表针跳向明天的那一刻,总有人蹲守。"),
                def("raffle_companion_1", null, 1, "机密档案", "T1", 5, true,
                        "metric_threshold", "raffle_companion_count", new BigDecimal("1"), "gte",
                        63, "陪跑", "报名 raffle 未中奖(仅计已开奖期次)", "没中奖,但你确实来了,这也算数。",
                        "不是所有到场的人都举得起奖杯。")));
        when(catalogPort.allSeriesLabels()).thenReturn(Map.of());
        when(unlockPort.myUnlocks(7)).thenReturn(List.of(
                new AchievementUnlock(7, "raffle_companion_1", Instant.now(), 5, "batch_scan", true)));
        when(metricPort.allMetrics(7)).thenReturn(Map.of());

        AchievementWallView wall = service.wall(7);

        AchievementCategoryView secret = wall.categories().get(0);
        assertThat(secret.hidden()).isTrue();
        assertThat(secret.series()).isEmpty(); // 机密档案恒不建系列分组,即便个别行带 tier_level

        AchievementItemView sealed = secret.items().stream()
                .filter(i -> i.code().equals("midnight_courier")).findFirst().orElseThrow();
        assertThat(sealed.name()).isNull();
        assertThat(sealed.condition()).isNull();
        assertThat(sealed.hint()).isEqualTo("表针跳向明天的那一刻,总有人蹲守。");

        AchievementItemView revealed = secret.items().stream()
                .filter(i -> i.code().equals("raffle_companion_1")).findFirst().orElseThrow();
        assertThat(revealed.name()).isEqualTo("陪跑");
        assertThat(revealed.condition()).isEqualTo("报名 raffle 未中奖(仅计已开奖期次)");
        assertThat(revealed.revealedLabel()).isEqualTo("曾是机密档案 #02"); // 该类目第 2 条(60=1st,63=2nd)
    }

    @Test
    void metaCategoryGoesToMetaAchievementsNotCategories() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("meta_regular", null, null, "元编年史", "T2", 15, false,
                        "metric_threshold", "achievement_unlock_total_count", new BigDecimal("10"), "gte",
                        70, "熟客认证", "累计点亮成就满 10 枚", "十枚成就到手,你已经不算新人了。", null)));
        when(catalogPort.allSeriesLabels()).thenReturn(Map.of());
        when(unlockPort.myUnlocks(1)).thenReturn(List.of());
        when(metricPort.allMetrics(1)).thenReturn(Map.of());

        AchievementWallView wall = service.wall(1);

        assertThat(wall.categories()).isEmpty();
        assertThat(wall.metaAchievements()).hasSize(1);
        assertThat(wall.metaAchievements().get(0).code()).isEqualTo("meta_regular");
    }

    @Test
    void pendingUnsealListsUnseenUnlocksOnly() {
        when(catalogPort.allDefinitions()).thenReturn(List.of(
                def("checkin_first", null, null, "入职档案", "T1", 5, false,
                        "metric_threshold", "checkin_total_count", new BigDecimal("1"), "gte",
                        1, "开机自检", "首次签到", "每天上班第一件事:证明我还活着。", null)));
        when(catalogPort.allSeriesLabels()).thenReturn(Map.of());
        when(unlockPort.myUnlocks(3)).thenReturn(List.of(
                new AchievementUnlock(3, "checkin_first", Instant.now(), 5, "batch_scan", false)));
        when(metricPort.allMetrics(3)).thenReturn(Map.of());

        AchievementWallView wall = service.wall(3);

        assertThat(wall.pendingUnseal()).containsExactly("checkin_first");
    }
}
