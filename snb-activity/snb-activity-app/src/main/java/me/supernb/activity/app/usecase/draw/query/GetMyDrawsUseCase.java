package me.supernb.activity.app.usecase.draw.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.MyDrawView;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.DrawPort;
import me.supernb.activity.domain.port.RechargeQueryPort;
import org.springframework.stereotype.Service;

/// 本人在本活动的中奖历史(含安慰奖),enrich 兑换码状态,面向本人不脱敏。无活动 → 空。
@Service
public class GetMyDrawsUseCase {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;
    private final RechargeQueryPort rechargePort;

    public GetMyDrawsUseCase(CampaignPort campaignPort, DrawPort drawPort, RechargeQueryPort rechargePort) {
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
