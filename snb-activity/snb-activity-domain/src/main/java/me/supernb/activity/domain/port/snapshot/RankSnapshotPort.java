package me.supernb.activity.domain.port.snapshot;

import java.time.LocalDate;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;

/// 榜单名次快照端口(每日定时任务写入,UsageBoardCache 刷新时读取用于计算升降箭头;Task 5 给 JDBC 实现)。
public interface RankSnapshotPort {

    /// 写入某周期/指标在 date 当天的名次快照:{userId -> rank},同日重跑覆盖(幂等)。
    void save(LocalDate date, BoardPeriod period, BoardMetric metric, Map<Long, Integer> ranks);

    /// 取 date 之前最近一次快照的名次映射;查无快照返回空 map。
    Map<Long, Integer> latestBefore(LocalDate date, BoardPeriod period, BoardMetric metric);

    /// 清理早于 cutoff 的历史快照(滚动保留窗口)。
    void purgeOlderThan(LocalDate cutoff);
}
