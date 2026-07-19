package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.WeightMode;

/// 管理端新建一期:prizes 为奖品骨架(克隆新建时携带上一期档位结构,payload 恒空,只经
/// 后续 generate-*/手填补上;空白新建传空列表)。返回新期 id。
public record CreateRaffleCampaignCommand(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
        GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
        WeightMode weightMode, List<PrizeSkeleton> prizes) implements Command<Long> {

    /// 奖品骨架:无 payload 字段——克隆时不可能把上一期真码带过来,新建请求 DTO 层面就没有
    /// 这条通路,不是靠"调用方别传"这种约定。
    public record PrizeSkeleton(String tier, String displayName, String kind, int sortOrder) {}
}
