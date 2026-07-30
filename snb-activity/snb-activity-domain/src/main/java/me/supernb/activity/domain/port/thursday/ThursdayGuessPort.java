package me.supernb.activity.domain.port.thursday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/// 猜桶竞猜存取端口。
public interface ThursdayGuessPort {

    /// 一条猜测(结算用:并列取最早提交,所以列表要带序)。
    ///
    /// @param userId 提交人
    /// @param guess  猜的份数
    record GuessRecord(long userId, int guess) {
    }

    /// 本人本场的猜测;没猜过为空。
    Optional<Integer> myGuess(LocalDate session, long userId);

    /// 本场猜测人数。
    long count(LocalDate session);

    /// 本场全部猜测,按提交时刻升序(并列判定靠这个顺序)。
    List<GuessRecord> all(LocalDate session);

    /// 落一条猜测;已猜过则原样返回旧值不覆盖(唯一键兜底,并发双击也只留第一条)。
    int submitOnce(LocalDate session, long userId, int guess);
}
