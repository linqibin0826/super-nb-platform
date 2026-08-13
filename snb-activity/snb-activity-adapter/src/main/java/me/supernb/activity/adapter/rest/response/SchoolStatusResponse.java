package me.supernb.activity.adapter.rest.response;

import java.util.List;
import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 开学季状态响应:白名单映射,只含**本人**的资格/领取态与匿名计数——
/// 无他人身份、无邮箱、无订单号(runbook 31 零 payload 纪律)。
/// open=false(休眠/未开始/领取截止后)时两块全为空态,前端进 cover 态。
public record SchoolStatusResponse(boolean open, String endsAtLabel,
        FirstCharge firstCharge, Invite invite) {

    /// 首充块。tierCard=0 即无资格;status ∈ none|claimable|pending|claimed|failed。
    public record FirstCharge(boolean charged, boolean inWindow, int tierCard,
            String amountCny, String status) {
    }

    /// 邀请块(count 已按里程碑口径封顶 10;榜单的不封顶计数走 /school/leaderboard)。
    public record Invite(int count, List<Milestone> milestones, boolean kfcUnlocked) {
    }

    /// 里程碑档。
    public record Milestone(int tier, int cardAmount, boolean unlocked, String status) {
    }

    public static SchoolStatusResponse of(SchoolStatusView v) {
        return new SchoolStatusResponse(v.open(), v.endsAtLabel(),
                new FirstCharge(v.firstCharge().charged(), v.firstCharge().inWindow(),
                        v.firstCharge().tierCard(), v.firstCharge().amountCny(), v.firstCharge().status()),
                new Invite(v.invite().count(),
                        v.invite().milestones().stream()
                                .map(m -> new Milestone(m.tier(), m.cardAmount(), m.unlocked(), m.status()))
                                .toList(),
                        v.invite().kfcUnlocked()));
    }
}
