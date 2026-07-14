package me.supernb.activity.domain.port.read;

import java.util.List;

/// 联动矩阵成就信号只读端口(本域 activity schema 原生 SQL)。
public interface RaffleGateAchievementSignalPort {

    /// 用户 id + 计数。
    record UserCount(long userId, long count) {
    }

    /// 全体用户报名 raffle 次数。
    List<UserCount> raffleEntryCounts();

    /// 全体用户 raffle 中奖次数。
    List<UserCount> raffleWinCounts();

    /// 全体用户"报名过已开奖期次但未中奖"的次数(仅计已开奖 status='drawn' 的期次)。
    List<UserCount> raffleCompanionCounts();

    /// 全体用户金票闸机中签次数。
    List<UserCount> gateWinCounts();

    /// 全体用户幸运余额包开卡次数。
    List<UserCount> drawcardCounts();
}
