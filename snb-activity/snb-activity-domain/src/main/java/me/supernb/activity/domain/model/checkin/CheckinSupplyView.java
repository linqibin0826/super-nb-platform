package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;
import java.util.List;

/// 补给资格进度总视图(GET /checkin/status.supply 的领域来源)。
///
/// @param monthlyRechargeCny 当月新增真实充值(元)
/// @param gaugePct           进度条百分比(0~100):刻度 0/A/B/C 依次立于 0%/33%/66%/100%,
///                           段内线性插值,四舍五入取整(2026-07-14 控制器裁决的分段刻度公式,
///                           替换 spec 草稿"朝下一档线性"公式;¥36 → 43 是与前端契约示例对齐的锚点)。
///                           每段另封顶到刻度线之下(32/65/99),未真正达标时四舍五入不会显示成
///                           下一档的整格刻度(2026-07-14 复审裁决,消除"满格却未达标"的视觉矛盾)
/// @param gaugeNote          成品提示文案(如"距 B 档还差 ¥14")
/// @param tiers              三档展示状态,按 A/B/C 顺序
public record CheckinSupplyView(BigDecimal monthlyRechargeCny, int gaugePct, String gaugeNote,
        List<CheckinSupplyTierView> tiers) {
}
