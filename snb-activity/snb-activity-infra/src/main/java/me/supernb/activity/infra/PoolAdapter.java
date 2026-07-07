package me.supernb.activity.infra;

import java.util.List;
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.PoolPort;
import me.supernb.activity.infra.jpa.PrizeSlotJpaRepository;
import org.springframework.stereotype.Repository;

/// PoolPort 实现:按档位统计奖池余量。只出份数,绝不带出 redeem_code / claimed_by。
@Repository
public class PoolAdapter implements PoolPort {

    private final PrizeSlotJpaRepository slots;

    public PoolAdapter(PrizeSlotJpaRepository slots) {
        this.slots = slots;
    }

    @Override
    public List<ActivityDto.PoolTier> pool(long campaignId) {
        return slots.poolByAmount(campaignId).stream()
                .map(v -> new ActivityDto.PoolTier(v.getAmount(), (int) v.getTotal(), (int) v.getAvailable()))
                .toList();
    }
}
