package me.supernb.activity.app.usecase.checkin.command;

import java.time.LocalDate;

/// 打卡成功领域事件:CheckInHandler 在首次打卡确认后发布(打卡事务已在 CheckinAdapter 提交)。
/// 成就侧同步监听消费,即时补写 checkin 指标并判定解锁——让"开机自检"等成就打卡当场亮,不必
/// 等次日 00:20 日频同步 + 每小时判定。checkin 只发布事实、不依赖成就侧(依赖箭头保持
/// achievement→checkin,与 CheckinMetricSyncJob 等生产者一致)。
public record UserCheckedInEvent(long userId, LocalDate day) {
}
