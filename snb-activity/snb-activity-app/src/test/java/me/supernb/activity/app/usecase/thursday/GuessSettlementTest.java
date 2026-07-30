package me.supernb.activity.app.usecase.thursday;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort.GuessRecord;
import org.junit.jupiter.api.Test;

/// 猜桶结算:最接近者胜、并列取最早提交、猜高猜低对称、无人参与不炸。
class GuessSettlementTest {

    private static GuessRecord g(long uid, int guess) {
        return new GuessRecord(uid, guess);
    }

    @Test
    void closestGuessWins() {
        List<GuessRecord> all = List.of(g(1, 3), g(2, 9), g(3, 20));
        assertThat(GuessSettlement.winner(all, 8)).contains(g(2, 9));
    }

    /// 🚨 并列时先提交者胜 —— 靠的是入参已按提交时刻升序 + 严格小于比较。
    /// 把比较写成 `<=` 这条立刻红(后提交的会顶掉先提交的)。
    @Test
    void tieGoesToTheEarlierSubmission() {
        List<GuessRecord> all = List.of(g(1, 6), g(2, 10));
        assertThat(GuessSettlement.winner(all, 8)).contains(g(1, 6));
    }

    /// 猜高猜低一视同仁:差 2 就是差 2,不因为猜多了就吃亏。
    @Test
    void overAndUnderAreSymmetric() {
        assertThat(GuessSettlement.winner(List.of(g(1, 12), g(2, 9)), 10)).contains(g(2, 9));
        assertThat(GuessSettlement.winner(List.of(g(1, 11), g(2, 7)), 10)).contains(g(1, 11));
    }

    @Test
    void exactHitWins() {
        assertThat(GuessSettlement.winner(List.of(g(1, 7), g(2, 8), g(3, 9)), 8)).contains(g(2, 8));
    }

    @Test
    void noGuessesMeansNoWinner() {
        assertThat(GuessSettlement.winner(List.of(), 8)).isEmpty();
    }

    /// 一个人猜、答案是 0 也要有赢家(冷清场次不能把结算整崩)。
    @Test
    void singleGuessAgainstZeroAnswer() {
        assertThat(GuessSettlement.winner(List.of(g(1, 30)), 0)).contains(g(1, 30));
    }
}
