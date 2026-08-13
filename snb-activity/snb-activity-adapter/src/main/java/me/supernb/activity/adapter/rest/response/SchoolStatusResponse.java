package me.supernb.activity.adapter.rest.response;

import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 包机活动状态响应:白名单映射,只含**本人**的资格/领取态与匿名计数——
/// 无他人身份、无邮箱、无订单号(runbook 31 零 payload 纪律)。
/// open=false(休眠/未开始/领取截止后)时两块全为空态,前端进 cover 态。
public record SchoolStatusResponse(boolean open, String endsAtLabel,
        FirstCharge firstCharge, Invite invite) {

    /// 首充块。tierCard=0 即无资格;status ∈ none|claimable|pending|claimed|failed。
    public record FirstCharge(boolean charged, boolean inWindow, int tierCard,
            String amountCny, String status) {
    }

    /// 邀请块(v2):count=合格被邀数(不封顶);card=邀请卡养成态;kfcUnlocked=计满 20 人。
    public record Invite(int count, Card card, boolean kfcUnlocked) {
    }

    /// 邀请卡。tier/tierName/cardAmount=已领档(0=未开卡);claimableTier>tier 时可领/可升;
    /// resetsAvailable=重置银行可用次数;subscriptionId=本人卡的订阅 id(页面拿它去用户侧
    /// subscriptions/progress 匹配额度进度,只回本人自己的,无隐私外溢)。
    public record Card(int tier, String tierName, int cardAmount,
            int claimableTier, String claimableName, int claimableCard,
            int resetsAvailable, int resetsUsed, long subscriptionId) {
    }

    public static SchoolStatusResponse of(SchoolStatusView v) {
        SchoolStatusView.CardBlock c = v.invite().card();
        return new SchoolStatusResponse(v.open(), v.endsAtLabel(),
                new FirstCharge(v.firstCharge().charged(), v.firstCharge().inWindow(),
                        v.firstCharge().tierCard(), v.firstCharge().amountCny(), v.firstCharge().status()),
                new Invite(v.invite().count(),
                        new Card(c.tier(), c.tierName(), c.cardAmount(),
                                c.claimableTier(), c.claimableName(), c.claimableCard(),
                                c.resetsAvailable(), c.resetsUsed(), c.subscriptionId()),
                        v.invite().kfcUnlocked()));
    }
}
