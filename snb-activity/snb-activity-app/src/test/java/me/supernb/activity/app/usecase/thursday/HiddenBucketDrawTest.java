package me.supernb.activity.app.usecase.thursday;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/// 隐藏款摇号:确定性、范围合法、不重复、桶少时降级、相邻桶数互不关联。
class HiddenBucketDrawTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 30);

    /// 🚨 这条是整个设计的地基:没有存储,靠的就是"同输入必同输出"。
    /// 一旦有人把 Random 换成 SecureRandom/ThreadLocalRandom,这条立刻红。
    @Test
    void isDeterministic() {
        List<Integer> a = HiddenBucketDraw.draw("s", DAY, 20, 3);
        for (int i = 0; i < 50; i++) {
            assertThat(HiddenBucketDraw.draw("s", DAY, 20, 3)).isEqualTo(a);
        }
    }

    @Test
    void picksDistinctNumbersInRangeSorted() {
        List<Integer> r = HiddenBucketDraw.draw("s", DAY, 20, 3);
        assertThat(r).hasSize(3).doesNotHaveDuplicates().isSorted().allMatch(n -> n >= 1 && n <= 20);
    }

    /// 桶比奖还少:人人有份,绝不越界抽出不存在的桶序。
    @Test
    void fewerBucketsThanPrizesMeansEveryoneWins() {
        assertThat(HiddenBucketDraw.draw("s", DAY, 2, 3)).containsExactly(1, 2);
        assertThat(HiddenBucketDraw.draw("s", DAY, 1, 3)).containsExactly(1);
    }

    @Test
    void noBucketsMeansNoWinners() {
        assertThat(HiddenBucketDraw.draw("s", DAY, 0, 3)).isEmpty();
        assertThat(HiddenBucketDraw.draw("s", DAY, 20, 0)).isEmpty();
    }

    /// 不同场次/不同盐 → 不同结果(否则三场疯四会开出同一组号)。
    @Test
    void differentSessionOrSaltGivesDifferentDraw() {
        List<Integer> base = HiddenBucketDraw.draw("s", DAY, 30, 3);
        assertThat(HiddenBucketDraw.draw("s", DAY.plusWeeks(1), 30, 3)).isNotEqualTo(base);
        assertThat(HiddenBucketDraw.draw("other", DAY, 30, 3)).isNotEqualTo(base);
    }

    /// 🚨 相邻桶数必须毫无关联——否则有人能靠"卡桶数"(临门加一单/拦一单)来挪动中奖号。
    /// 这是用 SHA-256 做种子而不是 hashCode 的理由。
    @Test
    void adjacentBucketCountsAreUncorrelated() {
        int same = 0;
        for (int n = 10; n < 40; n++) {
            if (HiddenBucketDraw.draw("s", DAY, n, 3).equals(HiddenBucketDraw.draw("s", DAY, n + 1, 3))) {
                same++;
            }
        }
        assertThat(same).as("相邻桶数摇出同一组号的次数应为 0").isZero();
    }

    /// 空盐也要能跑(未配 salt 的环境退化成公开可复算,而不是崩)。
    @Test
    void nullSaltIsTolerated() {
        assertThat(HiddenBucketDraw.draw(null, DAY, 20, 3)).hasSize(3);
    }
}
