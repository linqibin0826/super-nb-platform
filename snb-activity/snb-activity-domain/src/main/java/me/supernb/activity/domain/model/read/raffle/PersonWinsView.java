package me.supernb.activity.domain.model.read.raffle;

import java.time.Instant;
import java.util.List;

/// 公开中奖记录视图(按人聚合):脱敏名 + 历次中奖。
/// 查询坐标只用已公开的(已开奖期 id, 参会证号),不新增任何身份标识出口;
/// 类型上没有 payload/userId,想漏都漏不出去。
public record PersonWinsView(String displayName, List<Win> wins) {

    /// 单次中奖:期 + 档位 + 奖品名,全部是开奖通报里已公开的字段。
    public record Win(long campaignId, String campaignName, Instant drawnAt, String tier,
            String prizeDisplayName) {}
}
