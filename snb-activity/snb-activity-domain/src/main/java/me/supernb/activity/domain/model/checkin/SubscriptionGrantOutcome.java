package me.supernb.activity.domain.model.checkin;

import java.util.List;
import java.util.Map;

/// 订阅批量分配结果(委托 sub2api admin bulk-assign 的领域视图,脱离 sub2api 包类型)。
///
/// @param statuses 按用户 id 的分配状态("created"|"reused"|"failed")
/// @param errors   整批级别的错误信息(如 409 冲突文案)
public record SubscriptionGrantOutcome(Map<Long, String> statuses, List<String> errors) {
}
