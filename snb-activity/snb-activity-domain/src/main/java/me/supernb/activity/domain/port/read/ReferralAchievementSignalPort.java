package me.supernb.activity.domain.port.read;

import java.util.List;
import java.util.Map;

/// 拉新达人成就信号只读端口:全部邀请关系(不分活动窗口)。
public interface ReferralAchievementSignalPort {

    /// inviter_id -> 被邀请人 id 列表。
    Map<Long, List<Long>> allInviteeIdsByInviter();
}
