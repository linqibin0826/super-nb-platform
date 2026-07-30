package me.supernb.activity.app.usecase.thursday;

import java.util.List;

/// 疯四桶视图(状态查询与领取共用一个形状,前端两处渲染同一套字段)。
///
/// payload 纪律:只回**本人**的资格/桶序与全场计数,不含任何他人身份、邮箱、金额
/// (spec 2026-07-12 §6 零 payload 原则;活动安全扫描 runbook 31 ①③ 同款约束)。
/// 中奖桶序是**桶号**不是人,公开它不暴露任何身份。
///
/// @param open          今天是不是疯四场次(且已配好分组);false 时其余字段无意义
/// @param eligible      本人是否在本场资格名单内(当天单笔 ≥ 门槛,且到账顺序在桶上限内)
/// @param claimed       本人这一场是否已领到卡
/// @param bucketNo      本人桶序(1 起);不达标为 null
/// @param issued        本场已出桶数(达标人数,封顶 bucketLimit)——群里拱火播报的那个数
/// @param bucketLimit   本场桶上限
/// @param hiddenBuckets 隐藏款(瑞幸)中奖桶序;**未到开奖时刻为 null**(前端据此显示"待开奖")
/// @param hiddenWin     本人是否中隐藏款;未开奖或不达标恒 false
public record ThursdayBucketView(boolean open, boolean eligible, boolean claimed, Integer bucketNo,
        int issued, int bucketLimit, List<Integer> hiddenBuckets, boolean hiddenWin) {

    /// 非场次日(或未配分组)的休眠态:前端据此整块不渲染。
    public static ThursdayBucketView closed(int bucketLimit) {
        return new ThursdayBucketView(false, false, false, null, 0, bucketLimit, null, false);
    }

    /// 保留领取前的全部判定,只把"已领"翻成 true(领取成功后回给前端)。
    public ThursdayBucketView asClaimed() {
        return new ThursdayBucketView(open, eligible, true, bucketNo, issued, bucketLimit,
                hiddenBuckets, hiddenWin);
    }
}
