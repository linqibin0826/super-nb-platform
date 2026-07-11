package me.supernb.activity.app.usecase.usageboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.BoardView;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import org.junit.jupiter.api.Test;

class BoardAssemblerTest {

    static final Instant T = Instant.parse("2026-07-10T10:00:00Z");

    static UsageBoardRow row(long uid, long tokens, double cost) {
        return new UsageBoardRow(uid, "u" + uid, null, tokens, tokens / 10, cost);
    }

    static BoardDataset ds(List<UsageBoardRow> rows) {
        return BoardAssembler.assemble(rows, Map.of(), Map.of(), T, T.plusSeconds(3600));
    }

    @Test
    void ranksByMetricWithUserIdTieBreak() {
        BoardDataset d = ds(List.of(row(3, 100, 5), row(1, 100, 9), row(2, 200, 1)));
        assertThat(d.byTokens()).extracting(RankedRow::userId).containsExactly(2L, 1L, 3L); // 并列 100 按 uid ASC
        assertThat(d.byTokens()).extracting(RankedRow::rank).containsExactly(1, 2, 3);
        assertThat(d.byCost()).extracting(RankedRow::userId).containsExactly(1L, 3L, 2L);
    }

    @Test
    void percentileFloorsAgainstParticipants() {
        BoardDataset d = ds(List.of(row(1, 400, 1), row(2, 300, 1), row(3, 200, 1), row(4, 100, 1)));
        assertThat(d.byTokens().get(0).percentile()).isEqualTo(75); // (4-1)*100/4
        assertThat(d.byTokens().get(3).percentile()).isEqualTo(0);
    }

    @Test
    void deltaComesFromPreviousRankMap() {
        BoardDataset d = BoardAssembler.assemble(List.of(row(1, 100, 1), row(2, 200, 2)),
                Map.of(1L, 1, 2L, 5), Map.of(), T, null);
        // tokens 榜:uid2 第1(prev 5 → delta +4),uid1 第2(prev 1 → delta -1)
        assertThat(d.byTokens().get(0).delta()).isEqualTo(4);
        assertThat(d.byTokens().get(1).delta()).isEqualTo(-1);
    }

    @Test
    void newcomerDeltaIsNull() {
        BoardDataset d = BoardAssembler.assemble(List.of(row(9, 100, 1)), Map.of(), Map.of(), T, null);
        assertThat(d.byTokens().get(0).delta()).isNull();
    }

    @Test
    void viewForMemberInsideTop50HasMeAndNoNeighborhood() {
        BoardDataset d = ds(List.of(row(1, 300, 30), row(2, 200, 20), row(3, 100, 10)));
        BoardView v = BoardAssembler.view(d, BoardPeriod.WEEK, BoardMetric.TOKENS, 2, uid -> true);
        assertThat(v.me().rank()).isEqualTo(2);
        assertThat(v.me().gapToNext().tokens()).isEqualTo(100); // 距 uid1 差 100
        assertThat(v.me().behind().tokens()).isEqualTo(100);    // 身后 uid3 差 100
        assertThat(v.meStatus()).isNull();
        assertThat(v.neighborhood()).isEmpty();
        assertThat(v.participants()).isEqualTo(3);
    }

    @Test
    void viewOutsideTop50BuildsElevenRowNeighborhood() {
        List<UsageBoardRow> rows = new java.util.ArrayList<>();
        for (long uid = 1; uid <= 80; uid++) rows.add(row(uid, 1000 - uid, 1)); // uid=rank
        BoardView v = BoardAssembler.view(ds(rows), BoardPeriod.WEEK, BoardMetric.TOKENS, 60, uid -> true);
        assertThat(v.top()).hasSize(50);
        assertThat(v.neighborhood()).extracting(e -> e.rank())
                .containsExactly(55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65);
    }

    @Test
    void absentUserGetsMeStatusByEligibility() {
        BoardDataset d = ds(List.of(row(1, 100, 1)));
        assertThat(BoardAssembler.view(d, BoardPeriod.DAY, BoardMetric.TOKENS, 99, uid -> true).meStatus())
                .isEqualTo("no_usage");
        assertThat(BoardAssembler.view(d, BoardPeriod.DAY, BoardMetric.TOKENS, 99, uid -> false).meStatus())
                .isEqualTo("not_eligible");
    }

    @Test
    void amountMetricUsesTierGapInsteadOfPersonGap() {
        BoardDataset d = ds(List.of(row(1, 100, 12000), row(2, 50, 750)));
        BoardView v = BoardAssembler.view(d, BoardPeriod.ALL, BoardMetric.AMOUNT, 2, uid -> true);
        assertThat(v.me().gapToNext()).isNull();
        assertThat(v.me().gapToNextTier().tier()).isEqualTo("T_1K");
        assertThat(v.me().gapToNextTier().amount()).isEqualTo(250.0);
        assertThat(BoardAssembler.view(d, BoardPeriod.ALL, BoardMetric.AMOUNT, 1, uid -> true)
                .me().gapToNextTier()).isNull(); // 已是最高档
        // 对外行只有档位码,没有精确金额字段(类型层面保证,这里锚一下值)
        assertThat(v.top().get(0).costTier()).isEqualTo("T_10K");
    }

    @Test
    void costTierBoundaries() {
        assertThat(CostTiers.tier(0)).isEqualTo("T_0");
        assertThat(CostTiers.tier(10)).isEqualTo("T_10");
        assertThat(CostTiers.tier(99.99)).isEqualTo("T_10");
        assertThat(CostTiers.tier(100)).isEqualTo("T_100");
        assertThat(CostTiers.tier(500)).isEqualTo("T_500");
        assertThat(CostTiers.tier(1000)).isEqualTo("T_1K");
        assertThat(CostTiers.tier(5000)).isEqualTo("T_5K");
        assertThat(CostTiers.tier(10000)).isEqualTo("T_10K");
    }
}
