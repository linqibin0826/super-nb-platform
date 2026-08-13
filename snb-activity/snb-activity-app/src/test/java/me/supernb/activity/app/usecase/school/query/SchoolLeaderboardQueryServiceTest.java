package me.supernb.activity.app.usecase.school.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import org.junit.jupiter.api.Test;

/// 拉人榜查询:休眠返空且不碰读端口;榜单不封顶原样透传;顺序保持。
class SchoolLeaderboardQueryServiceTest {

    private static final Instant START = Instant.parse("2026-08-13T04:00:00Z");
    private static final Instant END = Instant.parse("2026-08-31T16:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-08-20T00:00:00Z");

    private final SchoolReadPort read = mock(SchoolReadPort.class);

    private SchoolLeaderboardQueryService svc() {
        SchoolSeasonProperties props = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 135L, 3, "school-season");
        return new SchoolLeaderboardQueryService(props, read);
    }

    @Test
    void dormantReturnsEmptyWithoutTouchingReadPort() {
        SchoolSeasonProperties dormant = new SchoolSeasonProperties(
                "", "", "", 0, 0, 0, 0, 0, 0, 0, 3, "school-season");
        SchoolLeaderboardQueryService svc = new SchoolLeaderboardQueryService(dormant, read);
        assertThat(svc.top(IN_WINDOW)).isEmpty();
        verifyNoInteractions(read);
    }

    @Test
    void afterClaimDeadlineReturnsEmpty() {
        assertThat(svc().top(Instant.parse("2026-09-03T00:00:00Z"))).isEmpty();
    }

    @Test
    void topKeepsOrderAndUncappedCounts() {
        when(read.topInviters(START, END, new BigDecimal("50"), 20)).thenReturn(List.of(
                new SchoolReadPort.InviterRank("zh***an@qq.com", 23, 40),
                new SchoolReadPort.InviterRank("***@gmail.com", 3, 5),
                new SchoolReadPort.InviterRank("d***e@qq.com", 0, 7)));
        List<SchoolLeaderboardQueryService.Entry> top = svc().top(IN_WINDOW);
        assertThat(top).hasSize(3);
        assertThat(top.get(0).name()).isEqualTo("zh***an@qq.com");
        assertThat(top.get(0).count()).isEqualTo(23);   // 榜单不封顶(里程碑封 10 是奖励口径)
        assertThat(top.get(0).invited()).isEqualTo(40); // 注册总数透传(展示列)
        assertThat(top.get(1).count()).isEqualTo(3);
        assertThat(top.get(2).count()).isZero();        // 只注册没充值的也上榜(垫底)
        assertThat(top.get(2).invited()).isEqualTo(7);
    }
}
