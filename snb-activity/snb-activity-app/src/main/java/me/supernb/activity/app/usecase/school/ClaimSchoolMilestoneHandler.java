package me.supernb.activity.app.usecase.school;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolMilestoneCommand;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 开学季带人里程碑领取编排:tier 白名单(1/3/6;KFC 档 10 人工发放无此路) →
/// 解锁校验(服务端按合格被邀计数重算) → 幂等短路 → 占位发卡(同首充礼流程)。
@Service
public class ClaimSchoolMilestoneHandler
        implements CommandHandler<ClaimSchoolMilestoneCommand, SchoolStatusView> {

    private static final Logger log = LoggerFactory.getLogger(ClaimSchoolMilestoneHandler.class);

    private final SchoolSeasonProperties props;
    private final SchoolStatusQueryService query;
    private final SchoolClaimPort claimPort;
    private final SubscriptionGrantPort grantPort;

    /// Spring 装配构造(ObjectProvider 消歧,疯四同款)。
    @Autowired
    public ClaimSchoolMilestoneHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolClaimPort claimPort, ObjectProvider<SubscriptionGrantPort> grantPortProvider) {
        this(props, query, claimPort, grantPortProvider.getIfAvailable());
    }

    /// 全参构造(测试直接注入 mock/null grantPort)。
    ClaimSchoolMilestoneHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolClaimPort claimPort, SubscriptionGrantPort grantPort) {
        this.props = props;
        this.query = query;
        this.claimPort = claimPort;
        this.grantPort = grantPort;
    }

    @Override
    public SchoolStatusView handle(ClaimSchoolMilestoneCommand command) {
        long userId = command.userId();
        int tier = command.tier();
        // milestoneGroup 对未知档(含 KFC 档 10)抛 IllegalArgumentException,白名单在此收口
        long groupId = props.milestoneGroup(tier);
        SchoolStatusView view = query.view(userId);
        if (!view.open()) {
            throw new IllegalStateException("开学季不在领取期");
        }
        SchoolStatusView.Milestone milestone = view.invite().milestones().stream()
                .filter(m -> m.tier() == tier)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知里程碑人数档:" + tier));
        if (!milestone.unlocked()) {
            throw new IllegalStateException("里程碑未解锁:还差 " + (tier - view.invite().count()) + " 个兄弟");
        }
        if (SchoolStatusView.STATUS_CLAIMED.equals(milestone.status())
                || SchoolStatusView.STATUS_PENDING.equals(milestone.status())) {
            return view;
        }
        SchoolGrantFlow.grant(grantPort, claimPort, log, "开学季里程碑",
                userId, SchoolClaimRecord.KIND_MILESTONE, tier, groupId,
                props.validityDays(), props.notes());
        return query.view(userId);
    }
}
