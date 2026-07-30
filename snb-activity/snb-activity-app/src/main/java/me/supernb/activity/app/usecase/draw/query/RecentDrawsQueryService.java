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

/// 最近真实中奖信息流(排除安慰奖),服务端算好展示名:**设过用户名的显示用户名,没设的才回退脱敏邮箱**
/// (2026-07-30 站长要求——一屏全是 `li***in@weityz.com` 既难看又没辨识度)。
/// 无进行中活动 → 空列表(前端优雅降级)。查无用户的行(如账号已注销)直接跳过,不拼出半条数据。
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

    /// 取活动内最近真实中奖(至多 500 条),按 userId 批量查展示名后关联;查无展示名的行在这一步被过滤掉。
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
        Map<Long, String> names = rechargePort.displayNamesByIds(ids);
        return winners.stream()
                .filter(w -> names.containsKey(w.userId()))
                .map(w -> new PublicDraw(names.get(w.userId()), w.amount()))
                .toList();
    }
}
