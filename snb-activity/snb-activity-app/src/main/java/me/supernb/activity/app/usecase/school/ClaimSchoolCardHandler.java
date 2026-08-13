package me.supernb.activity.app.usecase.school;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Instant;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolCardCommand;
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

/// 包机邀请卡开卡/升档编排(合一命令,跨档直升):
/// 应得档服务端按合格被邀数重算 → 幂等短路(应得 ≤ 已领) → assign 新档组拿订阅 id →
/// 落卡表 → 升档时 revoke 旧档订阅(失败只 warn——旧卡多活几天无害,新卡已到手)。
///
/// 顺序铁律:**先 assign 后落表**——assign 成功落表失败(崩溃)时,下次 claim 同组
/// assign 走 reuse 返回同一订阅,再落表即自愈;反序会出现「表说有卡、实际没发」的死状态。
/// 开卡并发仲裁 = school_card user_id 唯一约束(insert ON CONFLICT 幂等回落,
/// 并发双方 assign 同档组 reuse 同一订阅,无泄漏)。
@Service
public class ClaimSchoolCardHandler
        implements CommandHandler<ClaimSchoolCardCommand, SchoolStatusView> {

    private static final Logger log = LoggerFactory.getLogger(ClaimSchoolCardHandler.class);

    private final SchoolSeasonProperties props;
    private final SchoolStatusQueryService query;
    private final SchoolCardPort cardPort;
    private final SubscriptionGrantPort grantPort;

    /// Spring 装配构造(ObjectProvider 消歧,疯四同款)。
    @Autowired
    public ClaimSchoolCardHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolCardPort cardPort, ObjectProvider<SubscriptionGrantPort> grantPortProvider) {
        this(props, query, cardPort, grantPortProvider.getIfAvailable());
    }

    /// 全参构造(测试直接注入 mock/null grantPort)。
    ClaimSchoolCardHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolCardPort cardPort, SubscriptionGrantPort grantPort) {
        this.props = props;
        this.query = query;
        this.cardPort = cardPort;
        this.grantPort = grantPort;
    }

    @Override
    public SchoolStatusView handle(ClaimSchoolCardCommand command) {
        long userId = command.userId();
        SchoolStatusView view = query.view(userId);
        if (!view.open()) {
            throw new IllegalStateException("活动不在领取期");
        }
        int count = view.invite().count();
        int deserved = SchoolSeasonProperties.deservedTier(count);
        if (deserved == 0) {
            throw new IllegalStateException("还没带到兄弟:先带 1 个开卡");
        }
        Optional<SchoolCardRecord> existing = cardPort.find(userId);
        int current = existing.map(SchoolCardRecord::tier).orElse(0);
        if (deserved <= current) {
            return view;
        }
        if (grantPort == null) {
            log.error("邀请卡发卡失败:SubscriptionGrantPort 未装配(检查 sub2api.admin-key 配置),userId={}", userId);
            throw new IllegalStateException("发卡通道未配置");
        }
        long groupId = props.cardGroup(deserved);
        int days = props.cardValidityDays(Instant.now());
        long subscriptionId;
        try {
            subscriptionId = grantPort.assign(userId, groupId, days, props.notes());
        } catch (RuntimeException e) {
            log.error("邀请卡 assign 失败:userId={} tier={} groupId={}", userId, deserved, groupId, e);
            throw new IllegalStateException("发卡失败,请稍后重试", e);
        }
        if (existing.isPresent()) {
            cardPort.upgrade(existing.get().id(), deserved, subscriptionId);
            try {
                grantPort.revoke(existing.get().subscriptionId());
            } catch (RuntimeException e) {
                // 旧档卡收不走只损失其残余额度(自然到期),绝不因此让升档失败
                log.warn("升档后旧订阅 revoke 失败(容忍):userId={} oldSubscription={}",
                        userId, existing.get().subscriptionId(), e);
            }
            log.info("邀请卡升档:userId={} {}→{} subscription={}", userId, current, deserved, subscriptionId);
        } else {
            // empty = 并发对手已开卡;同档组 assign 是 reuse 同一订阅,按幂等回落
            cardPort.insert(userId, deserved, subscriptionId)
                    .ifPresentOrElse(
                            r -> log.info("邀请卡开卡:userId={} tier={} subscription={}",
                                    userId, deserved, subscriptionId),
                            () -> log.info("邀请卡并发开卡撞唯一约束,按幂等回落:userId={}", userId));
        }
        return query.view(userId);
    }
}
