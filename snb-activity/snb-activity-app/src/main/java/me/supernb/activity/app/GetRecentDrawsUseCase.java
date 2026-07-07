package me.supernb.activity.app;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.supernb.activity.domain.Campaign;
import org.springframework.stereotype.Service;

/// 最近真实中奖信息流(排除安慰奖),服务端用脱敏邮箱做展示名。无活动 → 空。
/// 找不到邮箱的行(如已注销)直接跳过,不返回半条数据。
@Service
public class GetRecentDrawsUseCase {

    private static final int LIMIT = 500;

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;
    private final RechargeQueryPort rechargePort;

    public GetRecentDrawsUseCase(CampaignPort campaignPort, DrawPort drawPort, RechargeQueryPort rechargePort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
        this.rechargePort = rechargePort;
    }

    public List<ActivityDto.PublicDraw> recentDraws() {
        Campaign c = campaignPort.activeCampaign().orElse(null);
        if (c == null) {
            return List.of();
        }
        List<ActivityDto.RawWinner> winners = drawPort.recentRealWinners(c.id(), LIMIT);
        if (winners.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = winners.stream().map(ActivityDto.RawWinner::userId).collect(Collectors.toSet());
        Map<Long, String> emails = rechargePort.maskedEmailsByIds(ids);
        return winners.stream()
                .filter(w -> emails.containsKey(w.userId()))
                .map(w -> new ActivityDto.PublicDraw(emails.get(w.userId()), w.amount()))
                .toList();
    }
}
