package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleDrawSummary;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.model.raffle.WinnerSampler;
import me.supernb.activity.domain.port.raffle.RaffleDrawPort;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleCampaignJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleEntryJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.RafflePrizeJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleCampaignEntity;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleEntryEntity;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// RaffleDrawPort 实现:开奖全程单事务(spec 红线)。
///
/// 事务体顺序:CAS 抢闸(active 转 drawn 原生 UPDATE,未命中=已开过,直接无操作)
/// 然后实时复核门槛+账龄(批量读 sub2api,不吃报名时快照——堵充值后报名再退款的套利)
/// 然后 WinnerSampler 不放回抽样(一人至多一件),按 sort_order 逐件归属(脏检查随提交落库),
/// 最后留痕报名数/取消数。中途任何异常整体回滚,status 回到 active,下一分钟轮询自动重试——
/// 绝不允许「已 drawn 但无中奖人」的半截态。
@Repository
public class RaffleDrawAdapter implements RaffleDrawPort {

    private final RaffleCampaignJpaRepository campaigns;
    private final RaffleEntryJpaRepository entries;
    private final RafflePrizeJpaRepository prizes;
    private final RaffleGateReadPort gatePort;
    private final TransactionTemplate txTemplate;

    /// 构造:注入三仓储、门槛读端口与事务管理器。
    public RaffleDrawAdapter(RaffleCampaignJpaRepository campaigns, RaffleEntryJpaRepository entries,
            RafflePrizeJpaRepository prizes, RaffleGateReadPort gatePort,
            PlatformTransactionManager txManager) {
        this.campaigns = campaigns;
        this.entries = entries;
        this.prizes = prizes;
        this.gatePort = gatePort;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public RaffleDrawSummary drawCampaign(long campaignId, RandomGenerator rng) {
        return txTemplate.execute(status -> doDraw(campaignId, rng));
    }

    /// 事务体,见类注释。
    private RaffleDrawSummary doDraw(long campaignId, RandomGenerator rng) {
        if (campaigns.markDrawn(campaignId) == 0) {
            return RaffleDrawSummary.skipped(campaignId);
        }
        RaffleCampaignEntity campaign = campaigns.findById(campaignId).orElseThrow();
        List<RaffleEntryEntity> entrants = entries.findByCampaignId(campaignId);

        List<WinnerSampler.Candidate> eligible = eligibleCandidates(campaign, entrants);
        int disqualified = entrants.size() - eligible.size();

        List<RafflePrizeEntity> prizeUnits = prizes.findByCampaignIdOrderBySortOrderAscIdAsc(campaignId);
        List<Long> winners = WinnerSampler.pick(eligible, prizeUnits.size(),
                WeightMode.valueOf(campaign.getWeightMode()), rng);
        Instant now = Instant.now();
        for (int i = 0; i < winners.size(); i++) {
            prizeUnits.get(i).assign(winners.get(i), now); // 大奖(sort_order 靠前)先归属
        }
        campaigns.recordDrawStats(campaignId, entrants.size(), disqualified);
        return new RaffleDrawSummary(campaignId, true, winners.size(), disqualified);
    }

    /// 实时复核:门槛值窗口固定 [gate_from, entry_close_at)(对全体一致)+可选账龄;
    /// 复核值同时就是 WEIGHTED 模式的权重。批量缺席(窗口内无流水/查无此人)按 0/不合格处理。
    private List<WinnerSampler.Candidate> eligibleCandidates(RaffleCampaignEntity campaign,
            List<RaffleEntryEntity> entrants) {
        if (entrants.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = entrants.stream().map(RaffleEntryEntity::getUserId).toList();
        Map<Long, BigDecimal> values = gatePort.gateValues(userIds,
                GateType.valueOf(campaign.getGateType()), campaign.getGateFrom(), campaign.getEntryCloseAt());
        Map<Long, Instant> registeredAts = campaign.getMinAccountAgeDays() == null
                ? Map.of() : gatePort.registeredAts(userIds);
        Instant ageCutoff = campaign.getMinAccountAgeDays() == null
                ? null : Instant.now().minus(Duration.ofDays(campaign.getMinAccountAgeDays()));

        return entrants.stream()
                .map(e -> new WinnerSampler.Candidate(e.getUserId(),
                        values.getOrDefault(e.getUserId(), BigDecimal.ZERO)))
                .filter(c -> c.weight().compareTo(campaign.getGateAmount()) >= 0)
                .filter(c -> ageCutoff == null || (registeredAts.containsKey(c.userId())
                        && !registeredAts.get(c.userId()).isAfter(ageCutoff)))
                .toList();
    }
}
