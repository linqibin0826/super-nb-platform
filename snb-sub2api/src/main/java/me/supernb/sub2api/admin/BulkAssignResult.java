package me.supernb.sub2api.admin;

import java.util.List;
import java.util.Map;

/// sub2api `/subscriptions/bulk-assign` 响应(防腐层契约):按用户 id 的状态映射
/// (created|reused|failed)+ 整批错误信息列表(如 409 冲突文案)。
public record BulkAssignResult(Map<Long, String> statuses, List<String> errors) {
}
