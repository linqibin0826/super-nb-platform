package me.supernb.activity.app.usecase.draw.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.MyDrawView;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.springframework.stereotype.Service;

/// 本人在本活动的中奖历史(含安慰奖),enrich 兑换码状态,面向本人不脱敏。无活动 → 空。
@Service
public class MyDrawsQueryService {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;
    private final RechargeReadPort rechargePort;

    /// 构造:注入活动/抽奖/充值读端口。
    public MyDrawsQueryService(CampaignPort campaignPort, DrawPort drawPort, RechargeReadPort rechargePort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
        this.rechargePort = rechargePort;
    }

    public List<MyDrawView> myDraws(long userId) {
        Campaign c = campaignPort.activeCampaign().orElse(null);
        if (c == null) {
            return List.of();
        }
        List<RawDraw> raws = drawPort.myRawDraws(c.id(), userId);
        if (raws.isEmpty()) {
            return List.of();
        }
        List<String> codes = raws.stream()
                .map(RawDraw::redeemCode)
                .filter(Objects::nonNull)
                .toList();
        Map<String, CodeStatus> statuses = rechargePort.codeStatuses(codes);
        return raws.stream()
                .map(r -> {
                    CodeStatus st = r.redeemCode() == null ? null : statuses.get(r.redeemCode());
                    return new MyDrawView(
                            r.amount(),
                            r.redeemCode(),
                            r.consolation(),
                            r.createdAt(),
                            st == null ? null : st.status(),
                            st == null ? null : st.expiresAt());
                })
                .toList();
    }
}
