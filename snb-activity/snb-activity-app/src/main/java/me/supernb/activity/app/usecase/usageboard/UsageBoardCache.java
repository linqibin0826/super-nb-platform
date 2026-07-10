package me.supernb.activity.app.usecase.usageboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.activity.domain.port.snapshot.RankSnapshotPort;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 用量榜进程内缓存:四周期(日/周/月/总)各一份 [BoardDataset],后台定时刷新,请求只读缓存、秒开。
/// 刷新失败保留上一次成功结果(stale-while-error)+ error 日志,偶发故障不打穿榜单页。
@Slf4j
@Service
public class UsageBoardCache {

    private final UsageBoardReadPort port;
    private final RankSnapshotPort snapshotPort;
    private final EnumMap<BoardPeriod, AtomicReference<BoardDataset>> slots = new EnumMap<>(BoardPeriod.class);

    public UsageBoardCache(UsageBoardReadPort port, RankSnapshotPort snapshotPort) {
        this.port = port;
        this.snapshotPort = snapshotPort;
        for (BoardPeriod period : BoardPeriod.values()) {
            slots.put(period, new AtomicReference<>());
        }
    }

    /// 读取当前缓存的数据集;未预热过返回 null(端点回 503,见 Task 6)。
    public BoardDataset dataset(BoardPeriod period) {
        return slots.get(period).get();
    }

    /// 刷新单个周期:查库→组装→swap;查库或组装异常时保留旧值并记 error 日志,不向上抛出。
    public void refresh(BoardPeriod period) {
        Instant now = Instant.now();
        long startedAt = System.currentTimeMillis();
        try {
            List<UsageBoardRow> rows = port.aggregate(BoardPeriods.start(period, now), now);
            LocalDate today = now.atZone(BoardPeriods.SHANGHAI).toLocalDate();
            Map<Long, Integer> prevTokens = safeLatestBefore(today, period, BoardMetric.TOKENS);
            Map<Long, Integer> prevCost = safeLatestBefore(today, period, BoardMetric.AMOUNT);
            BoardDataset dataset = BoardAssembler.assemble(rows, prevTokens, prevCost, now, BoardPeriods.endsAt(period, now));
            slots.get(period).set(dataset);
            log.info("用量榜刷新 period={} rows={} took={}ms", period, rows.size(), System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("刷新用量榜失败 period={}", period, e);
        }
    }

    /// latestBefore 抛异常按空 map 处理:箭头缺席不影响主榜(spec §12)。
    private Map<Long, Integer> safeLatestBefore(LocalDate today, BoardPeriod period, BoardMetric metric) {
        try {
            return snapshotPort.latestBefore(today, period, metric);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /// 日/周/月三个短周期:每 5 分钟刷新一次。
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void refreshShort() {
        refresh(BoardPeriod.DAY);
        refresh(BoardPeriod.WEEK);
        refresh(BoardPeriod.MONTH);
    }

    /// 总榜全表聚合成本较高:每 30 分钟刷新一次。
    @Scheduled(fixedDelay = 1_800_000, initialDelay = 1_800_000)
    public void refreshAll() {
        refresh(BoardPeriod.ALL);
    }

    /// 启动预热:应用就绪后串行刷新全部四个周期,避免冷启 miss。
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        for (BoardPeriod period : BoardPeriod.values()) {
            refresh(period);
        }
    }
}
