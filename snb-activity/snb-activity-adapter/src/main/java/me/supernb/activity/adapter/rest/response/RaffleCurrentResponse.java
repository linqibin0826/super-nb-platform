package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import me.supernb.activity.domain.model.read.raffle.RaffleCurrentView;

/// 当前期公开响应:serverNow 供倒计时对齐;campaign=null 表示无进行中。
/// 字段与读视图一一对应直接复制;雪花 id 转字符串、枚举值转小写。**没有 payload 字段。**
public record RaffleCurrentResponse(Instant serverNow, CampaignView campaign) {

    /// 组装:视图为 null 时 campaign 置 null。
    public static RaffleCurrentResponse of(Instant serverNow, RaffleCurrentView v) {
        return new RaffleCurrentResponse(serverNow, v == null ? null : CampaignView.of(v));
    }

    /// 期视图。
    public record CampaignView(String id, String name, Instant entryOpenAt, Instant entryCloseAt,
            Instant drawAt, String gateType, BigDecimal gateAmount, Instant gateFrom, String weightMode,
            String status, int entrantCount, List<Entrant> recentEntrants, List<PrizeLine> prizes) {

        static CampaignView of(RaffleCurrentView v) {
            return new CampaignView(String.valueOf(v.id()), v.name(), v.entryOpenAt(), v.entryCloseAt(),
                    v.drawAt(), v.gateType().toLowerCase(Locale.ROOT), v.gateAmount(), v.gateFrom(),
                    v.weightMode().toLowerCase(Locale.ROOT), v.status(), v.entrantCount(),
                    v.recentEntrants().stream()
                            .map(e -> new Entrant(e.entryNo(), e.displayName())).toList(),
                    v.prizes().stream()
                            .map(p -> new PrizeLine(p.tier(), p.displayName(),
                                    p.kind().toLowerCase(Locale.ROOT), p.count())).toList());
        }
    }

    /// 列席代表行。
    public record Entrant(int entryNo, String displayName) {}

    /// 议程单行(聚合计数)。
    public record PrizeLine(String tier, String displayName, String kind, int count) {}
}
