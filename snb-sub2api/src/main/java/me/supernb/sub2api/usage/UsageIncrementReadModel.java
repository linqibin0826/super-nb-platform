package me.supernb.sub2api.usage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/// sub2api 用量增量只读读模型:小时级增量扫描用(唯一禁止现查全表的轴,深化稿 §6.1)。
public interface UsageIncrementReadModel {

    /// 窗口 [since,until) 内每个用户的新增调用次数(增量,不是累计总数)。
    Map<Long, Long> callCountsSince(Instant since, Instant until);

    /// 窗口 [since,until) 内每个用户是否存在"凌晨1-4点(Asia/Shanghai)"的调用
    /// (仅返回 true 的条目,查无即视为 false)。
    Map<Long, Boolean> lateNightFlagsSince(Instant since, Instant until);

    /// 某天(指定时区自然日)每个用户的调用次数(日终结算峰值用,现查当天,不增量)。
    Map<Long, Long> callCountsOnDay(LocalDate day, ZoneId zone);
}
