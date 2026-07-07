package me.supernb.activity.infra.adapter.read;

import java.util.List;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.port.read.PoolReadPort;
import me.supernb.activity.infra.adapter.persistence.dao.PrizeSlotJpaRepository;
import org.springframework.stereotype.Repository;

/// PoolReadPort 实现:按档位统计奖池余量。只出份数,绝不带出 redeem_code / claimed_by。
@Repository
public class PoolReadAdapter implements PoolReadPort {

    private final PrizeSlotJpaRepository slots;

    /// 构造:注入奖槽仓储。
    public PoolReadAdapter(PrizeSlotJpaRepository slots) {
        this.slots = slots;
    }

    /// 委托 `PrizeSlotJpaRepository` 的 GROUP BY 投影统计各档位余量,把 SQL 计数(long)收窄为
    /// int 后映射为 [PoolTier]。
    @Override
    public List<PoolTier> pool(long campaignId) {
        return slots.poolByAmount(campaignId).stream()
                .map(v -> new PoolTier(v.getAmount(), (int) v.getTotal(), (int) v.getAvailable()))
                .toList();
    }
}
