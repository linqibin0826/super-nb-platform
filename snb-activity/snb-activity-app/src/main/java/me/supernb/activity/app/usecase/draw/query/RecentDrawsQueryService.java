package me.supernb.activity.app.usecase.draw.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.read.PublicDraw;
import me.supernb.activity.domain.model.read.RawWinner;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.springframework.stereotype.Service;

/// 最近真实中奖信息流(排除安慰奖),服务端用脱敏邮箱做展示名。无进行中活动 → 空列表(前端优雅降级)。
/// 查无邮箱的行(如账号已注销)直接跳过,不拼出半条数据。
@Service
public class RecentDrawsQueryService {

    private static final int LIMIT = 500;

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;
    private final RechargeReadPort rechargePort;

    /// 构造:注入活动/抽奖/充值读端口。
    public RecentDrawsQueryService(CampaignPort campaignPort, DrawPort drawPort, RechargeReadPort rechargePort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
        this.rechargePort = rechargePort;
    }

    /// 取活动内最近真实中奖(至多 500 条),按 userId 批量查脱敏邮箱后关联;查无邮箱的行在这一步被过滤掉。
    /// 无进行中活动 → 空列表。
    public List<PublicDraw> recentDraws() {
        Campaign c = campaignPort.activeCampaign().orElse(null);
        if (c == null) {
            return List.of();
        }
        List<RawWinner> winners = drawPort.recentRealWinners(c.id(), LIMIT);
        if (winners.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = winners.stream().map(RawWinner::userId).collect(Collectors.toSet());
        Map<Long, String> emails = rechargePort.maskedEmailsByIds(ids);
        return winners.stream()
                .filter(w -> emails.containsKey(w.userId()))
                .map(w -> new PublicDraw(emails.get(w.userId()), w.amount()))
                .toList();
    }
}
