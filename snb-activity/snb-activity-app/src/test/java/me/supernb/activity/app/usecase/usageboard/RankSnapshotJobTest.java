package me.supernb.activity.app.usecase.usageboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.activity.domain.port.snapshot.RankSnapshotPort;
import org.junit.jupiter.api.Test;

class RankSnapshotJobTest {

    record Saved(LocalDate date, BoardPeriod p, BoardMetric m, Map<Long, Integer> ranks) {}

    static class RecordingSnap implements RankSnapshotPort {
        List<Saved> saved = new ArrayList<>();
        LocalDate purgedBefore;
        @Override public void save(LocalDate d, BoardPeriod p, BoardMetric m, Map<Long, Integer> r) {
            saved.add(new Saved(d, p, m, r));
        }
        @Override public Map<Long, Integer> latestBefore(LocalDate d, BoardPeriod p, BoardMetric m) {
            return Map.of();
        }
        @Override public void purgeOlderThan(LocalDate cutoff) { purgedBefore = cutoff; }
    }

    static class RecordingPort implements UsageBoardReadPort {
        List<Instant[]> windows = new ArrayList<>();
        @Override public List<UsageBoardRow> aggregate(Instant s, Instant e) {
            windows.add(new Instant[] {s, e});
            return List.of(new UsageBoardRow(1, "u1", null, 100, 1, 1.0));
        }
        @Override public boolean eligible(long userId) { return true; }
    }

    @Test
    void snapshotsEightGroupsWithDayUsingYesterdayFullWindow() {
        RecordingPort port = new RecordingPort();
        RecordingSnap snap = new RecordingSnap();
        new RankSnapshotJob(port, snap).snapshotDaily();

        assertThat(snap.saved).hasSize(8);                        // 4 周期 × 2 指标
        LocalDate today = LocalDate.now(BoardPeriods.SHANGHAI);
        assertThat(snap.saved).allMatch(sv -> sv.date().equals(today));
        assertThat(snap.purgedBefore).isEqualTo(today.minusDays(30));

        // aggregate 每周期调一次(共 4 次,枚举序 DAY 最先):DAY 窗口=[昨日00:00,今日00:00) Asia/Shanghai
        assertThat(port.windows).hasSize(4);
        Instant todayStart = today.atStartOfDay(BoardPeriods.SHANGHAI).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(BoardPeriods.SHANGHAI).toInstant();
        assertThat(port.windows.get(0)[0]).isEqualTo(yesterdayStart);
        assertThat(port.windows.get(0)[1]).isEqualTo(todayStart);
        for (int i = 1; i < 4; i++) {
            assertThat(port.windows.get(i)[1]).isAfter(todayStart);   // 其余周期上界=当刻
        }
    }
}
