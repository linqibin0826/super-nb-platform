package me.supernb.sub2api.referral;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/// sub2api 拉新活动双榜只读读模型(防腐层契约)。全部方法只读,窗口统一 [start, end)(end 排他)。
///
/// 安全边界:榜单条目的 name 在实现内部完成邮箱脱敏,未脱敏的完整邮箱绝不跨出本模块。
/// 口径见各方法;两榜均排除 admin(role<>'user')、软删用户、inviter_id=1(站长自号)。
public interface ReferralReadModel {

    /// 充值榜条目(name 已脱敏)。
    ///
    /// @param name   邀请人脱敏标识
    /// @param total  被邀请新用户窗口内充值原始合计(元,排序依据)
    /// @param capped 匹配奖励封顶后的金额(min(total, cap))
    record RechargeRow(String name, BigDecimal total, BigDecimal capped) {
    }

    /// 人数榜条目(name 已脱敏)。
    ///
    /// @param name  邀请人脱敏标识
    /// @param count 有效邀请人数(曾开通新人组的被邀请人数)
    record InviteRow(String name, int count) {
    }

    /// 充值榜 Top limit:窗口内注册且经邀请链接注册的新用户,其窗口内 COMPLETED 余额充值按邀请人聚合,
    /// 原始总额降序;capped = min(total, cap)。
    List<RechargeRow> rechargeBoard(Instant start, Instant end, BigDecimal cap, int limit);

    /// 人数榜 Top limit:曾开通新人组(group_id=newcomerGroupId,不滤 deleted_at,曾开通即算)的
    /// 被邀请人数按邀请人聚合,人数降序。
    List<InviteRow> inviteBoard(long newcomerGroupId, int limit);

    /// 新人总数:窗口内注册([start,end))且未软删的用户数;只看注册,不要求进群/开通新人组/有邀请人。
    int newcomerTotal(Instant start, Instant end);

    /// 全量邀请关系(不分活动窗口):inviter_id -> 被邀请人 id 列表,排除站长自号(inviter_id=1)
    /// 与软删用户。referral_valid_count 成就用——"是否有效"(被邀者有首次成功调用)由调用方
    /// 交叉 user_metric 判断,本方法只给"谁邀请了谁"这个事实。
    java.util.Map<Long, java.util.List<Long>> allInviteeIdsByInviter();
}
