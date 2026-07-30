package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.port.read.ThursdayBucketReadPort;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.stereotype.Component;

/// ThursdayBucketReadPort 实现:薄适配,委托 snb-sub2api 的 RechargeReadModel。
@Component
public class ThursdayBucketReadAdapter implements ThursdayBucketReadPort {

    private final RechargeReadModel readModel;

    /// 构造:注入 starter 提供的充值只读模型(指向 sub2api 只读源)。
    public ThursdayBucketReadAdapter(RechargeReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<Long> qualifiedInOrder(Instant start, Instant end, BigDecimal minAmount, int limit) {
        return readModel.qualifiedUserIdsInOrder(start, end, minAmount, limit);
    }

    @Override
    public boolean alreadyClaimed(long userId, long groupId, String notes) {
        return readModel.hasSubscription(userId, groupId, notes);
    }
}
