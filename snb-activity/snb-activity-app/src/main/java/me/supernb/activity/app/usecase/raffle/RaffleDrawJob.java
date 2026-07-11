package me.supernb.activity.app.usecase.raffle;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.random.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RaffleDrawSummary;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleDrawPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 到点开奖任务:每 10 秒轮询「draw_at 已到且 active」的期——开奖时刻是库里的动态数据,
/// 不能编译期 cron 绑定;查询吃 idx_raffle_campaign_due,常态零命中零成本。
/// 10s 间隔是观感参数:页面到点后的「采样中」等待≈扫描间隔+页面 6s 轮询,
/// 60s 时最坏干等 66s(首期彩排实测 47s),压到 10s 后最坏 ~16s。
/// 系统自驱写路径不经 CommandBus(RankSnapshotJob 惯例);逐期 try/catch 互不拖垮;
/// 单事务与幂等由 RaffleDrawPort 实现保证,任务层只做调度与日志。
@Slf4j
@Service
public class RaffleDrawJob {

    private final RaffleCampaignPort campaignPort;
    private final RaffleDrawPort drawPort;
    private final RandomGenerator rng;

    /// Spring 装配构造(多构造器需显式 @Autowired 指认):生产随机源固定 SecureRandom。
    @Autowired
    public RaffleDrawJob(RaffleCampaignPort campaignPort, RaffleDrawPort drawPort) {
        this(campaignPort, drawPort, new SecureRandom());
    }

    /// 全参构造:测试注入固定种子随机源。
    RaffleDrawJob(RaffleCampaignPort campaignPort, RaffleDrawPort drawPort, RandomGenerator rng) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
        this.rng = rng;
    }

    /// 每分钟轮询入口。
    @Scheduled(fixedDelay = 10_000, initialDelay = 30_000)
    public void drawDue() {
        for (RaffleCampaign c : campaignPort.dueForDraw(Instant.now())) {
            try {
                RaffleDrawSummary s = drawPort.drawCampaign(c.id(), rng);
                if (s.executed()) {
                    log.info("发布会开奖 campaign={} winners={} disqualified={}",
                            s.campaignId(), s.winners(), s.disqualified());
                }
            } catch (Exception e) {
                log.error("发布会开奖失败 campaign={}", c.id(), e);
            }
        }
    }
}
