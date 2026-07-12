package me.supernb.activity.domain.port.raffle;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;

/// 奖品件端口。⚠️ 返回记录含机密 payload——只准 me/开奖路径消费,公开视图组装时不得透传。
public interface RafflePrizePort {

    /// 该期全部奖品件,按张榜顺序。
    List<RafflePrize> byCampaign(long campaignId);

    /// 本人在该期中的奖品件。
    Optional<RafflePrize> wonBy(long campaignId, long userId);

    /// 某用户在所有已开奖期的历次中奖(公开字段,不含 payload),按开奖时刻倒序。
    List<PersonWinsView.Win> winsOf(long userId);
}
