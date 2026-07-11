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

/// 到点开奖任务:每分钟轮询「draw_at 已到且 active」的期——开奖时刻是库里的动态数据,
/// 不能编译期 cron 绑定;查询吃 idx_raffle_campaign_due,常态零命中零成本。
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
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
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
