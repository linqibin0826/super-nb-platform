package me.supernb.activity.domain.port.school;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;

/// 开学季领取台账端口。判重唯一真源=school_claim 表,
/// 绝不用订阅 notes 匹配(疯四 alreadyClaimed 教训)。
public interface SchoolClaimPort {

    Optional<SchoolClaimRecord> find(long userId, String kind, int tier);

    List<SchoolClaimRecord> findByUser(long userId);

    /// 占位:插入 pending 行。撞 (user_id, kind, tier) 唯一约束(并发重复点击)时返回 empty,
    /// 调用方按幂等处理——单条 `ON CONFLICT DO NOTHING RETURNING id` 原子语句,无先查后插窗口。
    Optional<SchoolClaimRecord> insertPending(long userId, String kind, int tier, long groupId);

    void markSuccess(long id);

    /// 失败回写:累加 attempts、记录错误信息;failed 行允许再次领取时重试发卡。
    void markFailed(long id, String error);
}
