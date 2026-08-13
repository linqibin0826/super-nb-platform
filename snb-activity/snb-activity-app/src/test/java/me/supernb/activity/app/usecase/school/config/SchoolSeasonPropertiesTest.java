package me.supernb.activity.app.usecase.school.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/// 包机配置:空窗口/零组 id = 休眠;宽限默认 end+48h;坏日期启动期即炸(thursday 先例);
/// v2 新增=应得档/重置银行推导公式与邀请卡动态有效期。
class SchoolSeasonPropertiesTest {

    private SchoolSeasonProperties props(String start, String end, String deadline) {
        return new SchoolSeasonProperties(start, end, deadline,
                129L, 130L, 131L, 132L, 133L, 134L, 135L, 3, "school-season");
    }

    @Test
    void blankStartMeansDormant() {
        assertThat(props("", "2026-08-31T16:00:00Z", "").configured()).isFalse();
    }

    @Test
    void zeroGroupIdMeansDormant() {
        SchoolSeasonProperties p = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                0L, 130L, 131L, 132L, 133L, 134L, 135L, 3, "school-season");
        assertThat(p.configured()).isFalse();
        SchoolSeasonProperties p2 = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 0L, 3, "school-season");
        assertThat(p2.configured()).isFalse();
    }

    @Test
    void fullConfigIsConfigured() {
        assertThat(props("2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "").configured()).isTrue();
    }

    @Test
    void claimDeadlineDefaultsToEndPlus48h() {
        SchoolSeasonProperties p = props("2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "");
        assertThat(p.claimDeadline()).isEqualTo(Instant.parse("2026-09-02T16:00:00Z"));
    }

    @Test
    void explicitClaimDeadlineWins() {
        SchoolSeasonProperties p =
                props("2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "2026-09-01T16:00:00Z");
        assertThat(p.claimDeadline()).isEqualTo(Instant.parse("2026-09-01T16:00:00Z"));
    }

    @Test
    void groupLookupByTier() {
        SchoolSeasonProperties p = props("2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "");
        assertThat(p.firstChargeGroup(50)).isEqualTo(129L);
        assertThat(p.firstChargeGroup(100)).isEqualTo(130L);
        assertThat(p.firstChargeGroup(200)).isEqualTo(131L);
        assertThat(p.cardGroup(1)).isEqualTo(132L);
        assertThat(p.cardGroup(2)).isEqualTo(133L);
        assertThat(p.cardGroup(3)).isEqualTo(134L);
        assertThat(p.cardGroup(4)).isEqualTo(135L);
        assertThatThrownBy(() -> p.firstChargeGroup(70)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.cardGroup(5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.cardGroup(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deservedTierFollowsThresholds() {
        assertThat(SchoolSeasonProperties.deservedTier(0)).isZero();
        assertThat(SchoolSeasonProperties.deservedTier(1)).isEqualTo(1);   // Go
        assertThat(SchoolSeasonProperties.deservedTier(4)).isEqualTo(1);
        assertThat(SchoolSeasonProperties.deservedTier(5)).isEqualTo(2);   // Plus
        assertThat(SchoolSeasonProperties.deservedTier(9)).isEqualTo(2);
        assertThat(SchoolSeasonProperties.deservedTier(10)).isEqualTo(3);  // ProLite
        assertThat(SchoolSeasonProperties.deservedTier(19)).isEqualTo(3);
        assertThat(SchoolSeasonProperties.deservedTier(20)).isEqualTo(4);  // Pro
        assertThat(SchoolSeasonProperties.deservedTier(35)).isEqualTo(4);  // 封顶档不再涨
    }

    @Test
    void resetsEarnedSkipsNodes() {
        // earned(n) = n − |{节点 ≤ n}|,节点={1,5,10,20}:节点人头升档,非节点人头 +1 重置
        assertThat(SchoolSeasonProperties.resetsEarned(0)).isZero();
        assertThat(SchoolSeasonProperties.resetsEarned(1)).isZero();       // 第 1 人=开卡,无重置
        assertThat(SchoolSeasonProperties.resetsEarned(2)).isEqualTo(1);
        assertThat(SchoolSeasonProperties.resetsEarned(4)).isEqualTo(3);
        assertThat(SchoolSeasonProperties.resetsEarned(5)).isEqualTo(3);   // 第 5 人=升 Plus,不加
        assertThat(SchoolSeasonProperties.resetsEarned(9)).isEqualTo(7);
        assertThat(SchoolSeasonProperties.resetsEarned(10)).isEqualTo(7);  // 第 10 人=升 ProLite
        assertThat(SchoolSeasonProperties.resetsEarned(20)).isEqualTo(16); // 第 20 人=升 Pro
        assertThat(SchoolSeasonProperties.resetsEarned(25)).isEqualTo(21); // 20 后每人继续 +1 不封顶
    }

    @Test
    void cardValidityDaysCeilsToActivityEnd() {
        SchoolSeasonProperties p = props("2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "");
        // 正好整天
        assertThat(p.cardValidityDays(Instant.parse("2026-08-28T16:00:00Z"))).isEqualTo(3);
        // 不足整天向上取整(多送几小时,绝不早收)
        assertThat(p.cardValidityDays(Instant.parse("2026-08-28T15:00:00Z"))).isEqualTo(4);
        // 贴着结束至少 1 天
        assertThat(p.cardValidityDays(Instant.parse("2026-08-31T15:59:00Z"))).isEqualTo(1);
        assertThat(p.cardValidityDays(Instant.parse("2026-09-01T00:00:00Z"))).isEqualTo(1);
    }

    @Test
    void garbageStartFailsFast() {
        assertThatThrownBy(() -> props("not-a-date", "2026-08-31T16:00:00Z", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
