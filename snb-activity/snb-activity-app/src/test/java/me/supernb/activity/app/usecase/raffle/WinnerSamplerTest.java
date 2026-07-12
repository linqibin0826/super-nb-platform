package me.supernb.activity.app.usecase.raffle;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.model.raffle.WinnerSampler;
import me.supernb.activity.domain.model.raffle.WinnerSampler.Candidate;
import org.junit.jupiter.api.Test;

/// 抽样器纯函数:不放回、一人至多一次、数量=min(奖品,候选);加权模式重仓者概率显著占优。
class WinnerSamplerTest {

    private static List<Candidate> equalCandidates(int n) {
        return LongStream.rangeClosed(1, n)
                .mapToObj(uid -> new Candidate(uid, BigDecimal.ONE))
                .toList();
    }

    @Test
    void picksDistinctWinnersOfExpectedSize() {
        List<Long> winners = WinnerSampler.pick(equalCandidates(10), 4, WeightMode.EQUAL, new Random(7));
        assertThat(winners).hasSize(4).doesNotHaveDuplicates();
    }

    @Test
    void prizeCountBeyondCandidatesCapsAtCandidates() {
        List<Long> winners = WinnerSampler.pick(equalCandidates(3), 10, WeightMode.EQUAL, new Random(7));
        assertThat(winners).hasSize(3).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void emptyCandidatesYieldNoWinners() {
        assertThat(WinnerSampler.pick(List.of(), 5, WeightMode.EQUAL, new Random(7))).isEmpty();
    }

    @Test
    void sameSeedIsDeterministic() {
        assertThat(WinnerSampler.pick(equalCandidates(20), 5, WeightMode.EQUAL, new Random(42)))
                .isEqualTo(WinnerSampler.pick(equalCandidates(20), 5, WeightMode.EQUAL, new Random(42)));
    }

    @Test
    void weightedModeFavoursHeavyCandidateOverwhelmingly() {
        // 权重 1000 vs 1:单席位加权抽 1000 轮,重仓者应拿下绝大多数(理论 ~99.9%,阈值放 950 防抖)
        List<Candidate> pool = List.of(
                new Candidate(1L, new BigDecimal("1000")), new Candidate(2L, BigDecimal.ONE));
        Random rng = new Random(202607);
        int heavyWins = 0;
        for (int i = 0; i < 1000; i++) {
            if (WinnerSampler.pick(pool, 1, WeightMode.WEIGHTED, rng).getFirst() == 1L) {
                heavyWins++;
            }
        }
        assertThat(heavyWins).isGreaterThan(950);
    }

    @Test
    void equalModeIsRoughlyUniform() {
        // 5 选 1 平权抽 5000 轮,每人命中应落在 1000±150(3σ 内宽松带)
        Random rng = new Random(9);
        Map<Long, Integer> tally = new HashMap<>();
        for (int i = 0; i < 5000; i++) {
            long uid = WinnerSampler.pick(equalCandidates(5), 1, WeightMode.EQUAL, rng).getFirst();
            tally.merge(uid, 1, Integer::sum);
        }
        assertThat(tally).hasSize(5);
        tally.values().forEach(c -> assertThat(c).isBetween(850, 1150));
    }

    @Test
    void zeroTotalWeightFallsBackToUniform() {
        List<Candidate> pool = List.of(
                new Candidate(1L, BigDecimal.ZERO), new Candidate(2L, BigDecimal.ZERO));
        List<Long> winners = WinnerSampler.pick(pool, 2, WeightMode.WEIGHTED, new Random(7));
        assertThat(winners).containsExactlyInAnyOrder(1L, 2L);
    }
}
