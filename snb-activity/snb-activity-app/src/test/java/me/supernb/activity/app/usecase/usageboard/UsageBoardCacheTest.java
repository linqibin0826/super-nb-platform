package me.supernb.activity.app.usecase.usageboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.activity.domain.port.snapshot.RankSnapshotPort;
import org.junit.jupiter.api.Test;

class UsageBoardCacheTest {

    static class FakePort implements UsageBoardReadPort {
        List<UsageBoardRow> rows = List.of(new UsageBoardRow(1, "u1", null, 100, 1, 1.0));
        boolean explode = false;
        @Override public List<UsageBoardRow> aggregate(Instant s, Instant e) {
            if (explode) throw new RuntimeException("db down");
            return rows;
        }
        @Override public boolean eligible(long userId) { return true; }
    }

    static class FakeSnap implements RankSnapshotPort {
        @Override public void save(LocalDate d, BoardPeriod p, BoardMetric m, Map<Long, Integer> r) {}
        @Override public Map<Long, Integer> latestBefore(LocalDate d, BoardPeriod p, BoardMetric m) { return Map.of(); }
        @Override public void purgeOlderThan(LocalDate cutoff) {}
    }

    @Test
    void refreshPopulatesDatasetAndFailureKeepsOldValue() {
        FakePort port = new FakePort();
        UsageBoardCache cache = new UsageBoardCache(port, new FakeSnap());
        assertThat(cache.dataset(BoardPeriod.WEEK)).isNull();       // 未预热

        cache.refresh(BoardPeriod.WEEK);
        BoardDataset first = cache.dataset(BoardPeriod.WEEK);
        assertThat(first.participants()).isEqualTo(1);

        port.explode = true;
        cache.refresh(BoardPeriod.WEEK);                            // 失败:不抛、保旧值
        assertThat(cache.dataset(BoardPeriod.WEEK)).isSameAs(first);
    }
}
