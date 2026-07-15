package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.util.List;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyTierView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyView;

/// 签到状态响应:字段与领域读视图一一对应直接复制(家族惯例——读视图即对外契约的中间形态),
/// 形状按前端接线计划契约总览钉死。同一形状也是 POST /checkin 成功(200)响应体
/// (2026-07-14 控制器裁决:签到成功后返回完整状态快照,而非三字段结果)。
public record CheckinStatusResponse(
        boolean eligible,
        String ineligibleReason,
        boolean punchedToday,
        int todayDay,
        String monthLabel,
        int monthDays,
        List<Integer> checkedDays,
        int cumulativeDays,
        int streakCurrent,
        List<MilestoneLine> milestones,
        SupplyLine supply,
        int nbTotal) {

    public static CheckinStatusResponse of(CheckinStatusView v) {
        return new CheckinStatusResponse(v.eligible(), v.ineligibleReason(), v.punchedToday(), v.todayDay(),
                v.monthLabel(), v.monthDays(), v.checkedDays(), v.cumulativeDays(), v.streakCurrent(),
                v.milestones().stream().map(MilestoneLine::of).toList(), SupplyLine.of(v.supply()), v.nbTotal());
    }

    /// 单条里程碑。
    public record MilestoneLine(String code, String label, int target, boolean achieved, String statusText) {
        static MilestoneLine of(CheckinMilestoneView m) {
            return new MilestoneLine(m.code(), m.label(), m.target(), m.achieved(), m.statusText());
        }
    }

    /// 补给三档单档。
    public record TierLine(String tier, String label, String conditionText, BigDecimal thresholdCny,
            String state, String statusText) {
        static TierLine of(CheckinSupplyTierView t) {
            return new TierLine(t.tier(), t.label(), t.conditionText(), t.thresholdCny(), t.state(), t.statusText());
        }
    }

    /// 补给资格进度总视图。
    public record SupplyLine(BigDecimal monthlyRechargeCny, int gaugePct, String gaugeNote, List<TierLine> tiers) {
        static SupplyLine of(CheckinSupplyView s) {
            return new SupplyLine(s.monthlyRechargeCny(), s.gaugePct(), s.gaugeNote(),
                    s.tiers().stream().map(TierLine::of).toList());
        }
    }
}
