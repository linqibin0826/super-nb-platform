package me.supernb.activity.domain.port.nb;

/// NB 账本读端口:NB 值唯一真源 = SUM(nb_ledger.points)(EARN 正/SPEND 负,无行为 0)。
/// 写路径不设独立端口——打卡/解锁的账本行由各自持久化适配器同事务内联写入(原子),
/// 见 CheckinAdapter/AchievementUnlockAdapter;商城期 SPEND 再扩写端口。
public interface NbLedgerPort {

    /// 用户当前 NB 总值。
    int totalPoints(long userId);
}
