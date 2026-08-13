package me.supernb.activity.app.usecase.school;

import java.util.List;

/// 开学季状态视图(端点响应的内舱,adapter 层 Response 从它白名单映射)。
///
/// open 区间 = [start, claimDeadline)(宽限期仍可领);但资格事件只认 [start, end)。
/// status 取值:none(无资格/未解锁) | claimable | pending | claimed | failed(可重试)。
public record SchoolStatusView(boolean open, String endsAtLabel,
        FirstChargeBlock firstCharge, InviteBlock invite) {

    public static final String STATUS_NONE = "none";
    public static final String STATUS_CLAIMABLE = "claimable";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CLAIMED = "claimed";
    public static final String STATUS_FAILED = "failed";

    /// 首充块。charged=人生首充是否已发生(不论何时);inWindow=首充是否落窗口内;
    /// tierCard=0 表示无资格(没充过/窗口外/不足 ¥30),否则为卡面 50/100/200。
    public record FirstChargeBlock(boolean charged, boolean inWindow, int tierCard,
            String amountCny, String status) {
    }

    /// 邀请块。count 已按里程碑封顶截断;kfcUnlocked=计满 10 人(人工私聊发放,无 claim 端点)。
    public record InviteBlock(int count, List<Milestone> milestones, boolean kfcUnlocked) {
    }

    /// 里程碑档(tier=人数档 1/3/6,cardAmount=卡面 50/100/200)。
    public record Milestone(int tier, int cardAmount, boolean unlocked, String status) {
    }

    /// 休眠/窗口外的关闭态。
    public static SchoolStatusView closed() {
        return new SchoolStatusView(false, "",
                new FirstChargeBlock(false, false, 0, "", STATUS_NONE),
                new InviteBlock(0, List.of(), false));
    }
}
