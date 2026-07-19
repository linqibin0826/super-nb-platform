package me.supernb.activity.domain.port.raffle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;

/// 发布会期端口。
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

    /// 管理视图:全部期,不限状态,按报名开放时刻倒序(近期在前)。
    List<RaffleCampaign> listAll();

    /// 管理端新建一期,返回新期 id(status 固定 active)。
    long create(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            WeightMode weightMode);

    /// 管理端编辑标量字段(调用方负责校验可编辑性:status=="active")。
    void update(long id, String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            WeightMode weightMode);

    /// 作废:任意状态均可调用。
    void cancel(long id);
}
