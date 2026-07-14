package me.supernb.activity.app.usecase.checkin.query;

import java.time.format.DateTimeFormatter;
import java.util.List;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinRewardSummary;
import me.supernb.activity.domain.port.checkin.CheckinRewardPort;
import org.springframework.stereotype.Service;

/// 我的补给发放记录查询(GET /checkin/rewards,仅本人,spec §7.3 脱敏红线——
/// 不建"最近发放/全站台账"这类回吐他人明细的读模型)。只回历史已成功发放行,
/// 当月未结算不回占位行(结算 job 只在月末为"上个月"建行,天然满足契约要求)。
@Service
public class CheckinRewardQueryService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckinRewardPort rewardPort;
    private final CheckinTierProperties tierProps;

    /// 构造:注入发放台账端口与档位展示配置(取 label)。
    public CheckinRewardQueryService(CheckinRewardPort rewardPort, CheckinTierProperties tierProps) {
        this.rewardPort = rewardPort;
        this.tierProps = tierProps;
    }

    /// 用户本人已成功发放的记录,按月降序,附展示标签。
    public List<CheckinRewardSummary> myRewards(long userId) {
        return rewardPort.myGrantedRewards(userId).stream()
                .map(g -> new CheckinRewardSummary(g.grantMonth().format(MONTH_FORMAT), g.tier(),
                        tierProps.labelFor(g.tier()), g.grantedAt()))
                .toList();
    }
}
