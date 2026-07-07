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
import me.supernb.activity.domain.port.RechargeQueryPort;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.stereotype.Component;

/// RechargeQueryPort 实现:薄适配,委托 snb-sub2api 的 RechargeReadModel,把 sub2api 行映射为 activity app DTO。
@Component
public class RechargeQueryAdapter implements RechargeQueryPort {

    private final RechargeReadModel readModel;

    public RechargeQueryAdapter(RechargeReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public BigDecimal totalRecharge(long userId, Instant start, Instant end) {
        return readModel.totalRecharge(userId, start, end);
    }

    @Override
    public List<LeaderEntry> leaderboard(Instant start, Instant end, int limit) {
        return readModel.leaderboard(start, end, limit).stream()
                .map(r -> new LeaderEntry(r.name(), r.amount()))
                .toList();
    }

    @Override
    public List<RechargeEntry> recentRecharges(Instant start, Instant end, int limit) {
        return readModel.recentRecharges(start, end, limit).stream()
                .map(r -> new RechargeEntry(r.name(), r.amount(), r.at()))
                .toList();
    }

    @Override
    public Map<Long, String> maskedEmailsByIds(Collection<Long> ids) {
        return readModel.maskedEmailsByIds(ids);
    }

    @Override
    public Map<String, CodeStatus> codeStatuses(Collection<String> codes) {
        return readModel.codeStatuses(codes).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new CodeStatus(e.getValue().status(), e.getValue().expiresAt())));
    }
}
