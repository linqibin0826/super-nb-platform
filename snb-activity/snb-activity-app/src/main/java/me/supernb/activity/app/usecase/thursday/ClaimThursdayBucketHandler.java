package me.supernb.activity.app.usecase.thursday;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.activity.app.usecase.thursday.command.ClaimThursdayBucketCommand;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 疯四桶领取编排(spec §3):场次 → 资格(服务端重算,绝不信客户端) → 幂等短路 →
/// 委托 sub2api admin bulk-assign 发卡。
///
/// **资格每次都重算**:不缓存、不接受任何客户端传入的桶序或资格标记。桶序来自
/// 「当天单笔达标充值的到账顺序」,是库里的客观事实,前端只是显示它。
///
/// **失败必须响**:发卡是发钱,任何一环不确定都向上抛(500),绝不吞成"看起来领到了"。
/// 反面教材=QQ 引擎 cookie 失效后静默盲跑两天(runbook 23)、签到发放因 admin-key 缺席
/// 静默断(runbook 33)。
@Service
public class ClaimThursdayBucketHandler implements CommandHandler<ClaimThursdayBucketCommand, ThursdayBucketView> {

    private static final Logger log = LoggerFactory.getLogger(ClaimThursdayBucketHandler.class);

    private final ThursdayProperties props;
    private final ThursdayBucketQueryService query;
    private final SubscriptionGrantPort grantPort;

    /// Spring 装配构造:SubscriptionGrantPort 的唯一实现 `@ConditionalOnProperty(sub2api.admin-key)`,
    /// 未配 admin-key 的环境里该 Bean 根本不存在。用 ObjectProvider 取值——绝不能让无 admin-key
    /// 的环境因本 handler 直接注入而整个上下文起不来(照 CheckinMonthlySettlementJob 的 A-13 教训)。
    @Autowired
    public ClaimThursdayBucketHandler(ThursdayProperties props, ThursdayBucketQueryService query,
            ObjectProvider<SubscriptionGrantPort> grantPortProvider) {
        this(props, query, grantPortProvider.getIfAvailable());
    }

    /// 全参构造(测试直接注入 mock/null grantPort,照家族双构造器惯例)。
    ClaimThursdayBucketHandler(ThursdayProperties props, ThursdayBucketQueryService query,
            SubscriptionGrantPort grantPort) {
        this.props = props;
        this.query = query;
        this.grantPort = grantPort;
    }

    @Override
    public ThursdayBucketView handle(ClaimThursdayBucketCommand command) {
        long userId = command.userId();
        ThursdayBucketView view = query.view(userId);
        if (!view.open() || !view.eligible() || view.claimed()) {
            // 非场次 / 不达标 / 已领过:一律原样回视图,不发卡也不报错。
            // 已领过走这里而不是再调一次 bulk-assign——重复调虽然幂等(reused),
            // 但每次点击都打一发 admin API 是白白的抖动面。
            return view;
        }
        Long groupId = query.groupIdToday();
        if (groupId == null) {
            // view.open() 与本行之间跨了零点(场次翻篇)。极罕见,但宁可让这一次领取失败重来,
            // 也不能拿 null 分组去发卡。
            log.warn("疯四桶领取:判定与发卡之间场次已翻篇,userId={}", userId);
            return ThursdayBucketView.closed(props.bucketLimit());
        }
        if (grantPort == null) {
            log.error("疯四桶领取失败:SubscriptionGrantPort 未装配(检查 sub2api.admin-key 配置),userId={}", userId);
            throw new IllegalStateException("疯四桶发卡通道未配置");
        }

        SubscriptionGrantOutcome outcome =
                grantPort.bulkGrant(List.of(userId), groupId, props.validityDays(), props.notes());
        String status = outcome.statuses().getOrDefault(userId, "missing");
        if (!"created".equals(status) && !"reused".equals(status)) {
            log.error("疯四桶发卡未成功:userId={} groupId={} status={} errors={}",
                    userId, groupId, status, outcome.errors());
            throw new IllegalStateException("疯四桶发卡失败:" + status);
        }
        log.info("疯四桶已发:userId={} groupId={} 桶序={} status={}", userId, groupId, view.bucketNo(), status);
        return new ThursdayBucketView(true, true, true, view.bucketNo(), view.issued(), view.bucketLimit());
    }
}
