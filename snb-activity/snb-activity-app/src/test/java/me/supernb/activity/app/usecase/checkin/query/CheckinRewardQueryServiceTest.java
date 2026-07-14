package me.supernb.activity.app.usecase.checkin.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinGrantRecord;
import me.supernb.activity.domain.port.checkin.CheckinRewardPort;
import org.junit.jupiter.api.Test;

/// 我的补给发放记录查询:仅本人视角(GET /checkin/rewards),只回成功发放行 + 展示标签。
class CheckinRewardQueryServiceTest {

    private final CheckinRewardPort rewardPort = mock(CheckinRewardPort.class);
    private final CheckinTierProperties tierProps = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"));
    private final CheckinRewardQueryService service = new CheckinRewardQueryService(rewardPort, tierProps);

    @Test
    void mapsGrantRecordToSummaryWithLabelAndMonthString() {
        var record = new CheckinGrantRecord(LocalDate.of(2026, 6, 1), "B",
                Instant.parse("2026-06-30T15:58:00Z"));
        when(rewardPort.myGrantedRewards(42)).thenReturn(List.of(record));

        var summaries = service.myRewards(42);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).month()).isEqualTo("2026-06");
        assertThat(summaries.get(0).tier()).isEqualTo("B");
        assertThat(summaries.get(0).label()).isEqualTo("GPT-Pro 补给 · 3 天");
        assertThat(summaries.get(0).grantedAt()).isEqualTo(Instant.parse("2026-06-30T15:58:00Z"));
    }

    @Test
    void emptyWhenNoGrantedRewards() {
        when(rewardPort.myGrantedRewards(7)).thenReturn(List.of());
        assertThat(service.myRewards(7)).isEmpty();
    }
}
