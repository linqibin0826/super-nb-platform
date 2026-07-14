package me.supernb.activity.domain.port.metric;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// 用户指标底座端口(活动库):判定引擎与"原始业务表"之间的缓冲层。metric_code 是"查值接口",
/// 物化(落表)还是现查源表是按体量分档的实现细节,对 achievement_definition 透明
/// (深化稿 §6.1)。
public interface UserMetricPort {

    /// 写入/覆盖某用户某指标的当前值(纯 SQL upsert 天然幂等)。
    void upsert(long userId, String metricCode, double value);

    /// 批量写入(同一 metric_code,多用户;生产者日频/小时频批处理用)。
    void upsertBatch(String metricCode, Map<Long, Double> values);

    /// 单个用户单个指标的当前值;查无返回 empty。
    Optional<Double> value(long userId, String metricCode);

    /// 某用户全部指标(判定引擎逐用户判定时一次取齐,避免逐指标查询)。
    Map<String, Double> allMetrics(long userId);

    /// updated_at > since 的去重用户 id 列表(判定引擎的候选发现用)。
    List<Long> usersUpdatedSince(Instant since);
}
