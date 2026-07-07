package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.model.read.RechargeEntry;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.stereotype.Component;

/// RechargeReadPort 实现:薄适配,委托 snb-sub2api 的 RechargeReadModel,把上游行映射为
/// activity 自己的读视图。
@Component
public class RechargeReadAdapter implements RechargeReadPort {

    private final RechargeReadModel readModel;

    /// 构造:注入 starter 提供的充值读模型。
    public RechargeReadAdapter(RechargeReadModel readModel) {
        this.readModel = readModel;
    }

    /// 委托 starter 读模型统计区间充值合计。
    @Override
    public BigDecimal totalRecharge(long userId, Instant start, Instant end) {
        return readModel.totalRecharge(userId, start, end);
    }

    /// 委托 starter 读模型取区间充值榜 Top limit,映射为 [LeaderEntry](name 已在 starter 层脱敏)。
    @Override
    public List<LeaderEntry> leaderboard(Instant start, Instant end, int limit) {
        return readModel.leaderboard(start, end, limit).stream()
                .map(r -> new LeaderEntry(r.name(), r.amount()))
                .toList();
    }

    /// 委托 starter 读模型取区间最近充值流水,映射为 [RechargeEntry](name 已在 starter 层脱敏)。
    @Override
    public List<RechargeEntry> recentRecharges(Instant start, Instant end, int limit) {
        return readModel.recentRecharges(start, end, limit).stream()
                .map(r -> new RechargeEntry(r.name(), r.amount(), r.at()))
                .toList();
    }

    /// 委托 starter 读模型批量取脱敏邮箱,原样透传;找不到的 id 不在返回 map 中。
    @Override
    public Map<Long, String> maskedEmailsByIds(Collection<Long> ids) {
        return readModel.maskedEmailsByIds(ids);
    }

    /// 委托 starter 读模型批量取兑换码状态,映射为 [CodeStatus];找不到的 code 不在返回 map 中。
    @Override
    public Map<String, CodeStatus> codeStatuses(Collection<String> codes) {
        return readModel.codeStatuses(codes).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new CodeStatus(e.getValue().status(), e.getValue().expiresAt())));
    }
}
