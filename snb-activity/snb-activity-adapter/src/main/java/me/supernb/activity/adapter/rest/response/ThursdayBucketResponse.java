package me.supernb.activity.adapter.rest.response;

import java.util.List;
import me.supernb.activity.app.usecase.thursday.ThursdayBucketView;

/// 疯四桶响应:八字段白名单。只含**本人**的资格/桶序与全场计数——
/// 无他人身份、无邮箱、无金额、无名单(spec 2026-07-12 §6 零 payload;runbook 31 ①③ 同款约束)。
/// hiddenBuckets 是**桶号**不是人,公开它不暴露身份;未到开奖时刻为 null。
/// open=false(非场次日)时其余字段全为空态,前端整块不渲染。
public record ThursdayBucketResponse(boolean open, boolean eligible, boolean claimed, Integer bucketNo,
        int issued, int bucketLimit, List<Integer> hiddenBuckets, boolean hiddenWin) {

    public static ThursdayBucketResponse of(ThursdayBucketView v) {
        return new ThursdayBucketResponse(v.open(), v.eligible(), v.claimed(), v.bucketNo(),
                v.issued(), v.bucketLimit(), v.hiddenBuckets(), v.hiddenWin());
    }
}
