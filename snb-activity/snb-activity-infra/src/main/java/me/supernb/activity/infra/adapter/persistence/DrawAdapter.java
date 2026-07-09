package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawEligibility;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.model.read.RawWinner;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import me.supernb.activity.infra.adapter.persistence.dao.DrawJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.PrizeSlotJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.DrawEntity;
import me.supernb.activity.infra.adapter.persistence.entity.PrizeSlotEntity;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// DrawPort 实现:抽奖的原子事务收在这里。
///
/// 移植自 activity-svc draw.py 的并发安全语义:事务内先 pg_advisory_xact_lock(userId) 串行化
/// 同一用户,现查剩余次数(充值总额走只读端口,接受弱一致),再 FOR UPDATE SKIP LOCKED 原子领槽;
/// 池空则记安慰奖占位(campaign 配置的 consolationAmount,建表默认 5 元,不发码)。用
/// TransactionTemplate 显式包裹而非 @Transactional 注解,规避同类自调用时代理失效的坑。
@Repository
public class DrawAdapter implements DrawPort {

    private final DrawJpaRepository draws;
    private final PrizeSlotJpaRepository slots;
    private final TransactionTemplate txTemplate;
    private final RechargeReadPort rechargePort;

    /// 构造:注入抽奖与奖槽仓储、事务管理器(内部包成 TransactionTemplate)与充值只读端口。
    public DrawAdapter(DrawJpaRepository draws, PrizeSlotJpaRepository slots,
                       PlatformTransactionManager txManager, RechargeReadPort rechargePort) {
        this.draws = draws;
        this.slots = slots;
        this.txTemplate = new TransactionTemplate(txManager);
        this.rechargePort = rechargePort;
    }

    /// 事务内执行一次抽奖:advisory lock 串行化同一用户后委托 `doDraw`。
    @Override
    public DrawResult drawFor(Campaign campaign, long userId) {
        return txTemplate.execute(status -> doDraw(campaign, userId));
    }

    /// 事务体:校验资格 → SKIP LOCKED 随机领槽 → 落中奖/安慰奖记录。
    private DrawResult doDraw(Campaign campaign, long userId) {
        // 事务级 advisory lock:随事务结束自动释放,防并发超额
        draws.acquireUserXactLock(userId);

        BigDecimal total = rechargePort.totalRecharge(userId, campaign.startsAt(), campaign.endsAt());
        int used = countDraws(campaign.id(), userId);
        if (DrawEligibility.remainingDraws(total, used) <= 0) {
            throw new NoDrawsLeftException();
        }

        Optional<PrizeSlotEntity> claimed = slots.lockRandomAvailable(campaign.id());
        if (claimed.isPresent()) {
            PrizeSlotEntity slot = claimed.get();
            slot.claim(userId, Instant.now());
            draws.save(new DrawEntity(campaign.id(), userId, slot.getId(), slot.getAmount(),
                    slot.getRedeemCode(), false));
            return DrawResult.prize(slot.getAmount(), slot.getRedeemCode());
        }

        // 池空 → 安慰奖占位(不调 admin、不发码,人工发放)
        BigDecimal consolation = campaign.consolationAmount();
        draws.save(new DrawEntity(campaign.id(), userId, null, consolation, null, true));
        return DrawResult.consolation(consolation);
    }

    /// 事务内执行批量抽奖:advisory lock 串行化同一用户后委托 `doDrawAll`。
    @Override
    public List<DrawResult> drawAllFor(Campaign campaign, long userId) {
        return txTemplate.execute(status -> doDrawAll(campaign, userId));
    }

    /// 批量事务体:一把锁 → 现查剩余 → 循环 min(剩余, BATCH_MAX) 次领槽/记安慰奖。
    /// 每领一槽显式 slots.flush() 落库,否则下一次 native SKIP LOCKED 会重选同一槽(重复发码)。
    private List<DrawResult> doDrawAll(Campaign campaign, long userId) {
        draws.acquireUserXactLock(userId);

        BigDecimal total = rechargePort.totalRecharge(userId, campaign.startsAt(), campaign.endsAt());
        int used = countDraws(campaign.id(), userId);
        int quota = Math.min(DrawEligibility.BATCH_MAX, DrawEligibility.remainingDraws(total, used));
        if (quota <= 0) {
            throw new NoDrawsLeftException();
        }

        List<DrawResult> results = new ArrayList<>(quota);
        for (int i = 0; i < quota; i++) {
            Optional<PrizeSlotEntity> claimed = slots.lockRandomAvailable(campaign.id());
            if (claimed.isPresent()) {
                PrizeSlotEntity slot = claimed.get();
                slot.claim(userId, Instant.now());
                draws.save(new DrawEntity(campaign.id(), userId, slot.getId(), slot.getAmount(),
                        slot.getRedeemCode(), false));
                slots.flush();
                results.add(DrawResult.prize(slot.getAmount(), slot.getRedeemCode()));
            } else {
                BigDecimal consolation = campaign.consolationAmount();
                draws.save(new DrawEntity(campaign.id(), userId, null, consolation, null, true));
                results.add(DrawResult.consolation(consolation));
            }
        }
        return results;
    }

    /// 该活动内该用户的已抽次数。
    @Override
    public int countDraws(long campaignId, long userId) {
        return draws.countByCampaignIdAndUserId(campaignId, userId);
    }

    /// 该活动内本人的抽奖记录(含安慰奖),按创建时间倒序取最近 100 条并映射为 [RawDraw]
    /// (未 enrich 兑换码状态)。
    @Override
    public List<RawDraw> myRawDraws(long campaignId, long userId) {
        return draws.findTop100ByCampaignIdAndUserIdOrderByCreatedAtDesc(campaignId, userId).stream()
                .map(d -> new RawDraw(d.getAmount(), d.getRedeemCode(), d.isConsolation(),
                        d.getCreatedAt()))
                .toList();
    }

    /// 该活动排除安慰奖的中奖记录,按创建时间倒序取最近 limit 条并映射为 [RawWinner]
    /// (仅 userId+金额,未 enrich 邮箱)。
    @Override
    public List<RawWinner> recentRealWinners(long campaignId, int limit) {
        return draws.findByCampaignIdAndConsolationFalseOrderByCreatedAtDesc(campaignId, Limit.of(limit)).stream()
                .map(d -> new RawWinner(d.getUserId(), d.getAmount()))
                .toList();
    }
}
