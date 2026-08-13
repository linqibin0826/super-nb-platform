package me.supernb.activity.app.usecase.school;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolFirstChargeCommand;
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

/// 开学季首充礼领取编排:资格(服务端按人生首笔付款单重算,绝不信客户端) → 幂等短路 →
/// 领取表占位(ON CONFLICT 原子仲裁) → 委托 sub2api admin bulk-assign 发卡 → 状态回写。
///
/// **失败必须响**(疯四同纪律):发卡是发钱,任何一环不确定都向上抛,绝不吞成"看起来领到了";
/// 抛之前把台账置 failed,用户再点即走重试路径。
@Service
public class ClaimSchoolFirstChargeHandler
        implements CommandHandler<ClaimSchoolFirstChargeCommand, SchoolStatusView> {

    private static final Logger log = LoggerFactory.getLogger(ClaimSchoolFirstChargeHandler.class);

    private final SchoolSeasonProperties props;
    private final SchoolStatusQueryService query;
    private final SchoolClaimPort claimPort;
    private final SubscriptionGrantPort grantPort;

    /// Spring 装配构造:SubscriptionGrantPort 是 @ConditionalOnProperty(sub2api.admin-key) 的,
    /// 用 ObjectProvider 取值,别让无 admin-key 的环境因本 handler 起不来(疯四同款消歧)。
    @Autowired
    public ClaimSchoolFirstChargeHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolClaimPort claimPort, ObjectProvider<SubscriptionGrantPort> grantPortProvider) {
        this(props, query, claimPort, grantPortProvider.getIfAvailable());
    }

    /// 全参构造(测试直接注入 mock/null grantPort,家族双构造器惯例)。
    ClaimSchoolFirstChargeHandler(SchoolSeasonProperties props, SchoolStatusQueryService query,
            SchoolClaimPort claimPort, SubscriptionGrantPort grantPort) {
        this.props = props;
        this.query = query;
        this.claimPort = claimPort;
        this.grantPort = grantPort;
    }

    @Override
    public SchoolStatusView handle(ClaimSchoolFirstChargeCommand command) {
        long userId = command.userId();
        SchoolStatusView view = query.view(userId);
        if (!view.open()) {
            throw new IllegalStateException("开学季不在领取期");
        }
        SchoolStatusView.FirstChargeBlock fc = view.firstCharge();
        if (fc.tierCard() <= 0) {
            throw new IllegalStateException("没有首充礼资格");
        }
        if (SchoolStatusView.STATUS_CLAIMED.equals(fc.status())
                || SchoolStatusView.STATUS_PENDING.equals(fc.status())) {
            // 已领 / 发卡进行中:幂等回视图,不再打 admin API(白白的抖动面)。
            return view;
        }
        long groupId = props.firstChargeGroup(fc.tierCard());
        SchoolGrantFlow.grant(grantPort, claimPort, log, "开学季首充礼",
                userId, SchoolClaimRecord.KIND_FIRST_CHARGE, fc.tierCard(), groupId,
                props.validityDays(), props.notes());
        return query.view(userId);
    }
}
