package me.supernb.activity.domain.port.read;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/// 用量相关成就 metric 生产者用的只读端口(委托 sub2api 用量增量读模型)。
public interface UsageMetricSignalPort {

    /// 窗口 [since,until) 内每个用户的新增调用次数(增量)。
    Map<Long, Long> callCountsSince(Instant since, Instant until);

    /// 窗口 [since,until) 内存在深夜(上海 1-4 点)调用的用户(仅含 true 条目)。
    Map<Long, Boolean> lateNightFlagsSince(Instant since, Instant until);

    /// 某天(指定时区自然日)每个用户的调用次数(日终峰值用)。
    Map<Long, Long> callCountsOnDay(LocalDate day, ZoneId zone);
}
