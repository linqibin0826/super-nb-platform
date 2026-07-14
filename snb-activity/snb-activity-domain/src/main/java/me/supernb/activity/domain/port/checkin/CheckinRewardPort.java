package me.supernb.activity.domain.port.checkin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.checkin.CheckinGrantRecord;
import me.supernb.activity.domain.model.checkin.CheckinRewardCandidate;
import me.supernb.activity.domain.model.checkin.CheckinRewardView;

/// 补给资格发放台账端口(活动库):写读合一,照 CheckinPort/RaffleEntryPort 惯例。
public interface CheckinRewardPort {

    /// 占位声明本月档位:已存在则不新建(返回 empty,幂等,不因"想改档"而二次插入),
    /// 否则原子插入 pending 行返回其 id(spec §7.4)。
    Optional<Long> claim(long userId, LocalDate grantMonth, String tier, long groupId, String notes);

    /// 按状态取批(月度结算批处理用)。
    List<CheckinRewardCandidate> byStatus(String status);

    /// 标记发放成功。
    void markSuccess(long grantId);

    /// 标记发放失败(累加尝试次数,记录错误信息)。
    void markFailed(long grantId, String error);

    /// 标记预算硬顶打满,显式转入排队。
    void markDeferred(long grantId);

    /// 用户本人的全部发放记录,按 grant_month 降序。
    List<CheckinRewardView> myGrants(long userId);

    /// 用户本人已成功发放的记录(仅 status=success,按 grant_month 降序)。当月若尚未结算,
    /// 不会出现在返回值里——月度结算 job 只在月末为"上个月"建行,当月天然无记录(GET /checkin/rewards 用)。
    List<CheckinGrantRecord> myGrantedRewards(long userId);
}
