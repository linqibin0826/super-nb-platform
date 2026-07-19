package me.supernb.activity.domain.port.raffle;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;

/// 奖品件端口。⚠️ 返回记录含机密 payload——只准 me/开奖路径/管理端消费,公开视图组装时不得透传。
public interface RafflePrizePort {

    /// 该期全部奖品件,按张榜顺序。
    List<RafflePrize> byCampaign(long campaignId);

    /// 本人在该期中的奖品件。
    Optional<RafflePrize> wonBy(long campaignId, long userId);

    /// 某用户在所有已开奖期的历次中奖(公开字段,不含 payload),按开奖时刻倒序。
    List<PersonWinsView.Win> winsOf(long userId);

    /// 管理端新增一件奖品,返回新奖品 id。
    long create(long campaignId, String tier, String displayName, String kind, String payload, int sortOrder);

    /// 管理端编辑本件(调用方负责校验所属期可编辑性)。
    void update(long prizeId, String tier, String displayName, String kind, String payload, int sortOrder);

    /// 仅回填 payload(生成兑换码/口令后写入,tier/displayName/sortOrder 不动)。
    void updatePayload(long prizeId, String payload);

    /// 删除本件。
    void delete(long prizeId);

    /// 管理端批量新增(单事务,同批全部成功或全部回滚;生成兑换码用)。
    List<Long> createBatch(long campaignId, String tier, String displayName, String kind,
            List<String> payloads, int sortOrderStart);
}
