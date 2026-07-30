package me.supernb.activity.app.usecase.thursday;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort.GuessRecord;

/// 猜桶竞猜结算:最接近实际出桶数者胜,**并列取最早提交**。
///
/// 纯函数、不落表——赢家随时可由「本场全部猜测 + 实际桶数」重算出来,
/// 因此天然不可能改判(spec §6:规则公示后不改判)。与隐藏款摇号同一个思路。
public final class GuessSettlement {

    private GuessSettlement() {
    }

    /// 从按提交时刻升序的猜测里选出赢家;无人参与返回空。
    ///
    /// 入参必须**已按提交时刻升序**:并列时的「先提交者胜」完全靠这个顺序,
    /// 用严格小于比较保证同差值时不会被后来者顶掉。
    public static Optional<GuessRecord> winner(List<GuessRecord> guessesInOrder, int answer) {
        GuessRecord best = null;
        int bestGap = Integer.MAX_VALUE;
        for (GuessRecord g : guessesInOrder) {
            int gap = Math.abs(g.guess() - answer);
            if (gap < bestGap) {
                bestGap = gap;
                best = g;
            }
        }
        return Optional.ofNullable(best);
    }
}
