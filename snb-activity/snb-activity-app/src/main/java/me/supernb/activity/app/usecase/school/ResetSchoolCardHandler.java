package me.supernb.activity.app.usecase.school;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.app.usecase.school.command.ResetSchoolCardCommand;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 重置银行消耗编排(Tibo 时刻):先原子扣次数(库内 `used < earned` 谓词,并发双击只有
/// 一个扣到)→ 调 sub2api reset-quota 三窗全清 → 下游失败把次数回补再抛(失败不吞、
/// 次数不丢,疯四 claim 同纪律)。获得侧永远从合格人数推导,earned 不落库。
@Service
public class ResetSchoolCardHandler
        implements CommandHandler<ResetSchoolCardCommand, SchoolStatusView> {

    private static final Logger log = LoggerFactory.getLogger(ResetSchoolCardHandler.class);

    private final SchoolStatusQueryService query;
    private final SchoolCardPort cardPort;
    private final SubscriptionGrantPort grantPort;

    /// Spring 装配构造(ObjectProvider 消歧,疯四同款)。
    @Autowired
    public ResetSchoolCardHandler(SchoolStatusQueryService query, SchoolCardPort cardPort,
            ObjectProvider<SubscriptionGrantPort> grantPortProvider) {
        this(query, cardPort, grantPortProvider.getIfAvailable());
    }

    /// 全参构造(测试直接注入 mock/null grantPort)。
    ResetSchoolCardHandler(SchoolStatusQueryService query, SchoolCardPort cardPort,
            SubscriptionGrantPort grantPort) {
        this.query = query;
        this.cardPort = cardPort;
        this.grantPort = grantPort;
    }

    @Override
    public SchoolStatusView handle(ResetSchoolCardCommand command) {
        long userId = command.userId();
        SchoolStatusView view = query.view(userId);
        if (!view.open()) {
            throw new IllegalStateException("活动不在领取期");
        }
        SchoolCardRecord card = cardPort.find(userId)
                .orElseThrow(() -> new IllegalStateException("还没开卡:先带 1 个兄弟领卡"));
        int earned = SchoolSeasonProperties.resetsEarned(view.invite().count());
        if (card.resetsUsed() >= earned) {
            throw new IllegalStateException("重置次数不够:再带 1 个兄弟就有");
        }
        if (grantPort == null) {
            log.error("重置失败:SubscriptionGrantPort 未装配(检查 sub2api.admin-key 配置),userId={}", userId);
            throw new IllegalStateException("重置通道未配置");
        }
        if (!cardPort.consumeReset(card.id(), earned)) {
            throw new IllegalStateException("重置次数不够:再带 1 个兄弟就有");
        }
        try {
            grantPort.resetQuota(card.subscriptionId());
        } catch (RuntimeException e) {
            cardPort.refundReset(card.id());
            log.error("reset-quota 调用失败已回补次数:userId={} subscription={}",
                    userId, card.subscriptionId(), e);
            throw new IllegalStateException("重置没成功,次数已退回,请稍后重试", e);
        }
        log.info("邀请卡重置额度:userId={} subscription={} used={}→{}",
                userId, card.subscriptionId(), card.resetsUsed(), card.resetsUsed() + 1);
        return query.view(userId);
    }
}
