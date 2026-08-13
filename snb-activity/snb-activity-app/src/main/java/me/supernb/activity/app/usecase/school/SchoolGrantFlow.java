package me.supernb.activity.app.usecase.school;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.slf4j.Logger;

/// 开学季两条领取线共用的「占位→发卡→回写」流程(仅包内使用)。
///
/// 顺序铁律:先落 pending 台账再打 admin API——占位是并发仲裁(ON CONFLICT 原子),
/// 发卡失败置 failed 后**向上抛**,绝不吞;failed 行再次领取时不重复占位、直接重试发卡。
/// bulk-assign 的 `reused` 也算成功:同组 expired 订阅续期激活是预期行为(疯四 08-01 实证)。
final class SchoolGrantFlow {

    private SchoolGrantFlow() {
    }

    static void grant(SubscriptionGrantPort grantPort, SchoolClaimPort claimPort, Logger log,
            String what, long userId, String kind, int tier, long groupId,
            int validityDays, String notes) {
        if (grantPort == null) {
            log.error("{}发卡失败:SubscriptionGrantPort 未装配(检查 sub2api.admin-key 配置),userId={}",
                    what, userId);
            throw new IllegalStateException(what + "发卡通道未配置");
        }
        // failed 行复用(重试不重复占位);无行则占位;占位撞约束=并发对手正在发,幂等静默返回
        Optional<SchoolClaimRecord> existing = claimPort.find(userId, kind, tier);
        SchoolClaimRecord rec = existing.isPresent() ? existing.get()
                : claimPort.insertPending(userId, kind, tier, groupId).orElse(null);
        if (rec == null) {
            log.info("{}并发占位撞约束,按幂等回落:userId={} kind={} tier={}", what, userId, kind, tier);
            return;
        }
        if (SchoolClaimRecord.STATUS_SUCCESS.equals(rec.grantStatus())) {
            return;
        }
        try {
            SubscriptionGrantOutcome outcome =
                    grantPort.bulkGrant(List.of(userId), groupId, validityDays, notes);
            String status = outcome.statuses().getOrDefault(userId, "missing");
            if (!"created".equals(status) && !"reused".equals(status)) {
                log.error("{}发卡未成功:userId={} groupId={} status={} errors={}",
                        what, userId, groupId, status, outcome.errors());
                claimPort.markFailed(rec.id(), status + " " + outcome.errors());
                throw new IllegalStateException(what + "发卡失败:" + status);
            }
            claimPort.markSuccess(rec.id());
            log.info("{}已发:userId={} kind={} tier={} groupId={} status={}",
                    what, userId, kind, tier, groupId, status);
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("{}发卡异常:userId={} groupId={}", what, userId, groupId, e);
            claimPort.markFailed(rec.id(), e.getMessage());
            throw new IllegalStateException(what + "发卡失败,请稍后重试", e);
        }
    }
}
