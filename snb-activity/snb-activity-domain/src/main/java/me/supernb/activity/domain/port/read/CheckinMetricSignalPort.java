package me.supernb.activity.domain.port.read;

import java.time.LocalDate;
import java.util.List;

/// 签到成就 metric 生产者用的批量信号只读端口(读既有 checkin_record 表;与 Plan A 的
/// CheckinPort 服务不同消费场景——CheckinPort 服务单用户签到写读,本端口服务批量候选发现,
/// 两者共享同一张表是本仓多读模型格局的正常做法,如 GateRechargeReadPort 与
/// RaffleGateReadPort 都读 sub2api 充值数据但服务不同调用方)。
public interface CheckinMetricSignalPort {

    /// 某天签到过的全部用户 id(今日候选发现——只有今天签到的用户,累计/绝版/诈尸类
    /// metric 才可能变化,不必扫全表)。
    List<Long> usersCheckedInOn(LocalDate day);

    /// 某天签到过的用户里,签到时刻本地(Asia/Shanghai)落在 [00:00:00,00:01:00) 的用户 id
    /// (零点信使;60 秒窗口是对"0点整"的宽松解释,精确到毫秒不现实)。
    List<Long> usersCheckedInAtMidnightOn(LocalDate day);

    /// 用户最近两次签到(含 asOf 当天)日期间隔是否 ≥30 天(诈尸打卡)。
    boolean hasGhostReturnAsOf(long userId, LocalDate asOf);

    /// [from,to] 闭区间内至少签到一次的用户 id(创刊号候选发现)。
    List<Long> usersCheckedInBetween(LocalDate from, LocalDate to);
}
