package me.supernb.activity.app.usecase.school.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/// 开学季配置:空窗口/零组 id = 休眠;宽限默认 end+48h;坏日期启动期即炸(thursday 先例)。
class SchoolSeasonPropertiesTest {

    private SchoolSeasonProperties props(String start, String end, String deadline) {
        return new SchoolSeasonProperties(start, end, deadline,
                129L, 130L, 131L, 132L, 133L, 134L, 3, "school-season");
    }

    @Test
    void blankStartMeansDormant() {
        assertThat(props("", "2026-08-31T16:00:00Z", "").configured()).isFalse();
    }

    @Test
    void zeroGroupIdMeansDormant() {
        SchoolSeasonProperties p = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                0L, 130L, 131L, 132L, 133L, 134L, 3, "school-season");
        assertThat(p.configured()).isFalse();
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
        assertThat(p.milestoneGroup(1)).isEqualTo(132L);
        assertThat(p.milestoneGroup(3)).isEqualTo(133L);
        assertThat(p.milestoneGroup(6)).isEqualTo(134L);
        assertThatThrownBy(() -> p.firstChargeGroup(70)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.milestoneGroup(10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void garbageStartFailsFast() {
        assertThatThrownBy(() -> props("not-a-date", "2026-08-31T16:00:00Z", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
