package me.supernb.activity.domain.model.read.raffle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/// 当前期公开视图:期信息+列席人数+最近列席+议程单(奖品按档聚合计数,无 payload)。
public record RaffleCurrentView(long id, String name, Instant entryOpenAt, Instant entryCloseAt,
        Instant drawAt, String gateType, BigDecimal gateAmount, Instant gateFrom, String weightMode,
        String status, int entrantCount, List<Entrant> recentEntrants, List<PrizeLine> prizes) {

    /// 列席代表(脱敏展示名)。
    public record Entrant(int entryNo, String displayName) {}

    /// 议程单一行:同档同名同形态的奖品聚合计数。
    public record PrizeLine(String tier, String displayName, String kind, int count) {}
}
