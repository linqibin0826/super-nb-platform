package me.supernb.activity.domain.port.scan;

import java.time.Instant;
import java.util.Optional;

/// 批处理水位线端口(活动库):按 job_name 隔离,供签到与成就系统的多个批处理任务共享
/// (表 checkin_scan_watermark 在 Plan A V8 建,本计划是新消费方,不新建表)。
public interface ScanWatermarkPort {

    /// 取某任务的水位线;从未运行过返回 empty(调用方按"很久以前"的默认值兜底)。
    Optional<Instant> get(String jobName);

    /// 推进某任务的水位线(upsert)。
    void advance(String jobName, Instant to);
}
