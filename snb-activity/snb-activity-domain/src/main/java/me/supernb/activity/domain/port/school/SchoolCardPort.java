package me.supernb.activity.domain.port.school;

import java.util.Optional;
import me.supernb.activity.domain.model.school.SchoolCardRecord;

/// 包机邀请卡端口(玩法 v2)。一人一张卡,user_id 唯一约束=开卡并发仲裁真源;
/// 重置银行只落 resets_used,获得侧永远从合格人数推导。
public interface SchoolCardPort {

    Optional<SchoolCardRecord> find(long userId);

    /// 开卡:插入邀请卡。撞 user_id 唯一约束(并发重复开卡)返回 empty,调用方按幂等处理
    /// ——单条 `ON CONFLICT DO NOTHING RETURNING id` 原子语句,无先查后插窗口。
    Optional<SchoolCardRecord> insert(long userId, int tier, long subscriptionId);

    /// 升档:更新档位与新订阅 id(换组重发后)。
    void upgrade(long id, int tier, long subscriptionId);

    /// 消耗一次重置(原子):`resets_used < maxEarned` 条件内 +1,返回是否扣到
    /// ——并发双击同一枚重置时只有一个成功。
    boolean consumeReset(long id, int maxEarned);

    /// 重置回补:下游 reset-quota 调用失败时把扣掉的次数还回去(floor 0)。
    void refundReset(long id);
}
