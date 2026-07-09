package me.supernb.activity.app.usecase.referral.query;

import java.util.List;
import me.supernb.activity.app.usecase.referral.config.ReferralProperties;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;
import me.supernb.activity.domain.port.read.ReferralReadPort;
import org.springframework.stereotype.Service;

/// 拉新双榜查询用例:按 [ReferralProperties] 配置的窗口/新人组/封顶/榜长,委托 [ReferralReadPort]
/// 取充值榜与人数榜。窗口固定来自配置(独立于抽奖 Campaign),两榜公开免登录,name 已在防腐层脱敏。
@Service
public class ReferralLeaderboardQueryService {

    private final ReferralReadPort port;
    private final ReferralProperties props;

    /// 构造:注入拉新读端口与活动配置。
    public ReferralLeaderboardQueryService(ReferralReadPort port, ReferralProperties props) {
        this.port = port;
        this.props = props;
    }

    /// 充值榜 Top(配置 limit):窗口内被邀请新用户充值按邀请人聚合,原始总额降序。
    public List<ReferralRechargeEntry> rechargeBoard() {
        return port.rechargeBoard(props.getStart(), props.getEnd(), props.getCap(), props.getLimit());
    }

    /// 人数榜 Top(配置 limit):曾开通新人组的被邀请人数按邀请人聚合,人数降序。
    public List<ReferralInviteEntry> inviteBoard() {
        return port.inviteBoard(props.getNewcomerGroupId(), props.getLimit());
    }
}
