package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/// 疯四桶只读端口(sub2api 库):判「谁达标、桶序第几、这一场领过没」。
/// 窗口一律 [start, end),end 为排他上界。
public interface ThursdayBucketReadPort {

    /// 本场资格名单,按**到账顺序**排好、最多 limit 个;下标 +1 = 桶序。
    /// 排序键是充值到账时刻(不是领取时刻)——规则公示后不改判(spec §3/§6)。
    List<Long> qualifiedInOrder(Instant start, Instant end, BigDecimal minAmount, int limit);

    /// 该用户这一场领过没(按分组 + 固定 notes 判)。
    boolean alreadyClaimed(long userId, long groupId, String notes);
}
