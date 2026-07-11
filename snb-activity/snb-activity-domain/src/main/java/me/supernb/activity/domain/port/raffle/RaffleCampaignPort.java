package me.supernb.activity.domain.port.raffle;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;

/// 发布会期查询端口。
public interface RaffleCampaignPort {

    /// 当前展示期:最新一期非 cancelled(active 或 drawn)——开完的期停留展示
    /// 开奖结果供迟到访客重放,直到下一期开放顶替。
    Optional<RaffleCampaign> current();

    /// 按 id 取期(任意状态;cancelled 由调用方按需过滤)。
    Optional<RaffleCampaign> byId(long id);

    /// 到点待开奖:status=active 且 draw_at <= now。
    List<RaffleCampaign> dueForDraw(Instant now);

    /// 历届已开奖,按开奖时刻倒序取 limit 期。
    List<RaffleCampaign> drawnHistory(int limit);
}
