package me.supernb.activity.app.usecase.registry.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.supernb.activity.app.usecase.referral.config.ReferralProperties;
import me.supernb.activity.domain.model.read.registry.RegistryEntryStatus;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import org.springframework.stereotype.Service;

/// 活动中心注册表状态聚合(公开只读,spec 2026-07-12 §6):四个已知活动位各自的运行状态。
/// 字段白名单只有 id/kind/status/时间窗——绝不携带人数/名单/金额/身份,加字段须先改 spec。
/// lottery/raffle 无当前期时条目缺席(前端对缺席 id 静默无徽章);qq 窗口由配置推导;
/// leaderboard 常驻恒 running。查询代价=两次索引点查+纯内存,照 leaderboard/pool 先例不加缓存。
@Service
public class RegistryStatusQueryService {

    private final CampaignPort campaignPort;
    private final RaffleCampaignPort rafflePort;
    private final ReferralProperties referral;

    public RegistryStatusQueryService(
            CampaignPort campaignPort, RaffleCampaignPort rafflePort, ReferralProperties referral) {
        this.campaignPort = campaignPort;
        this.rafflePort = rafflePort;
        this.referral = referral;
    }

    public List<RegistryEntryStatus> status() {
        Instant now = Instant.now();
        List<RegistryEntryStatus> out = new ArrayList<>();
        campaignPort.activeCampaign().ifPresent(c -> out.add(new RegistryEntryStatus(
                "lottery", "lottery", phase(now, c.startsAt(), c.endsAt()), c.startsAt(), c.endsAt())));
        rafflePort.current().ifPresent(r -> out.add(new RegistryEntryStatus(
                "raffle", "raffle",
                r.drawn() ? "ended" : phase(now, r.entryOpenAt(), r.drawAt()),
                r.entryOpenAt(), r.drawAt())));
        out.add(new RegistryEntryStatus("qq-referral", "qq-referral",
                phase(now, referral.getStart(), referral.getEnd()), referral.getStart(), referral.getEnd()));
        out.add(new RegistryEntryStatus("leaderboard", "evergreen", "running", null, null));
        return List.copyOf(out);
    }

    /// 窗口判定 [start, end) 左闭右开,与仓库口径一致。
    private static String phase(Instant now, Instant start, Instant end) {
        if (now.isBefore(start)) {
            return "upcoming";
        }
        return now.isBefore(end) ? "running" : "ended";
    }
}
