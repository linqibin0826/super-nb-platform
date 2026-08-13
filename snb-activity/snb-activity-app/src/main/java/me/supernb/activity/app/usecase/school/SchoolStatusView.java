package me.supernb.activity.app.usecase.school;

/// 包机活动状态视图(端点响应的内舱,adapter 层 Response 从它白名单映射)。
///
/// open 区间 = [start, claimDeadline)(宽限期仍可领);但资格事件只认 [start, end)。
/// 首充线 status 取值:none(无资格) | claimable | pending | claimed | failed(可重试)。
///
/// 邀请线 v2 =「邀请卡养成 + 重置银行」:一人一张卡,tier=已领档位(0=未开卡),
/// claimableTier=应得档位(>tier 时可开卡/升档,跨档直升);重置获得侧从合格人数推导。
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

    /// 邀请块。count=合格被邀数(不封顶);kfcUnlocked=计满 20 人(人工私聊发放,无 claim 端点)。
    public record InviteBlock(int count, CardBlock card, boolean kfcUnlocked) {
    }

    /// 邀请卡块。tier/tierName/cardAmount=已领档(0/""/0=未开卡);
    /// claimableTier=应得档(> tier 时「领卡/升档」按钮亮);
    /// resetsAvailable=重置银行可用(earned(count) − used,earned 推导制)。
    public record CardBlock(int tier, String tierName, int cardAmount,
            int claimableTier, String claimableName, int claimableCard,
            int resetsAvailable, int resetsUsed) {

        /// 空卡块(未开卡且无资格)。
        public static CardBlock empty() {
            return new CardBlock(0, "", 0, 0, "", 0, 0, 0);
        }
    }

    /// 休眠/窗口外的关闭态。
    public static SchoolStatusView closed() {
        return new SchoolStatusView(false, "",
                new FirstChargeBlock(false, false, 0, "", STATUS_NONE),
                new InviteBlock(0, CardBlock.empty(), false));
    }
}
