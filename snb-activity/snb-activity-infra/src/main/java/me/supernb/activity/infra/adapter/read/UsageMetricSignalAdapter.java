package me.supernb.activity.infra.adapter.read;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import me.supernb.activity.domain.port.read.UsageMetricSignalPort;
import me.supernb.sub2api.usage.UsageIncrementReadModel;
import org.springframework.stereotype.Component;

/// UsageMetricSignalPort 实现:零逻辑薄委托 UsageIncrementReadModel。
@Component
public class UsageMetricSignalAdapter implements UsageMetricSignalPort {

    private final UsageIncrementReadModel readModel;

    public UsageMetricSignalAdapter(UsageIncrementReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public Map<Long, Long> callCountsSince(Instant since, Instant until) {
        return readModel.callCountsSince(since, until);
    }

    @Override
    public Map<Long, Boolean> lateNightFlagsSince(Instant since, Instant until) {
        return readModel.lateNightFlagsSince(since, until);
    }

    @Override
    public Map<Long, Long> callCountsOnDay(LocalDate day, ZoneId zone) {
        return readModel.callCountsOnDay(day, zone);
    }
}
