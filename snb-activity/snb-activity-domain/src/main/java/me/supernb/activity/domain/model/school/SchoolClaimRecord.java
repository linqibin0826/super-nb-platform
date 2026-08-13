package me.supernb.activity.domain.model.school;

/// 开学季领取台账记录(activity.school_claim 的领域视图)。
///
/// kind=first_charge 时 tier=卡面(50/100/200);kind=milestone 时 tier=人数档(1/3/6)。
/// grant_status 状态机:pending → success | failed(failed 可重试)。
public record SchoolClaimRecord(
        Long id,
        long userId,
        String kind,
        int tier,
        long groupId,
        String grantStatus,
        int attempts,
        String lastError) {

    public static final String KIND_FIRST_CHARGE = "first_charge";
    public static final String KIND_MILESTONE = "milestone";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
}
