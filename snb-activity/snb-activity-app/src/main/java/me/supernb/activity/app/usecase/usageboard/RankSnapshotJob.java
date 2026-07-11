package me.supernb.activity.app.usecase.usageboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.activity.domain.port.snapshot.RankSnapshotPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 每日 00:05(Asia/Shanghai)落八组名次快照(4 周期 × 2 指标):day 记「昨日终榜」
/// (昨日全天重算,spec §10),week/month/all 记任务时刻名次;随后清理 30 天前旧快照。
/// 任一周期失败只记日志不影响其余组——箭头缺席可接受(spec §12)。
@Slf4j
@Service
public class RankSnapshotJob {

    private static final int RETENTION_DAYS = 30;

    private final UsageBoardReadPort readPort;
    private final RankSnapshotPort snapshotPort;

    /// 构造:注入用量读端口与快照端口。
    public RankSnapshotJob(UsageBoardReadPort readPort, RankSnapshotPort snapshotPort) {
        this.readPort = readPort;
        this.snapshotPort = snapshotPort;
    }

    /// 每日快照入口(cron 固定上海时区,勿依赖容器 TZ)。
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    public void snapshotDaily() {
        LocalDate today = LocalDate.now(BoardPeriods.SHANGHAI);
        Instant now = Instant.now();
        Instant todayStart = today.atStartOfDay(BoardPeriods.SHANGHAI).toInstant();
        for (BoardPeriod period : BoardPeriod.values()) {
            try {
                Instant start = period == BoardPeriod.DAY
                        ? today.minusDays(1).atStartOfDay(BoardPeriods.SHANGHAI).toInstant()
                        : BoardPeriods.start(period, now);
                Instant end = period == BoardPeriod.DAY ? todayStart : now;
                List<UsageBoardRow> rows = readPort.aggregate(start, end);
                snapshotPort.save(today, period, BoardMetric.TOKENS, BoardAssembler.rankByTokens(rows));
                // 金额榜只开日/周(端点同规则拒 month/all),月/总的金额快照永远无消费方,不落
                if (period == BoardPeriod.DAY || period == BoardPeriod.WEEK) {
                    snapshotPort.save(today, period, BoardMetric.AMOUNT, BoardAssembler.rankByCost(rows));
                }
            } catch (Exception e) {
                log.error("名次快照失败 period={}", period, e);
            }
        }
        try {
            snapshotPort.purgeOlderThan(today.minusDays(RETENTION_DAYS));
        } catch (Exception e) {
            log.error("快照清理失败", e);
        }
    }
}
