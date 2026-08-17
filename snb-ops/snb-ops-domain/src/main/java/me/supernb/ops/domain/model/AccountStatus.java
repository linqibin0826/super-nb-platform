package me.supernb.ops.domain.model;

/// 邮箱账号自身状态(与名下订阅状态独立):可用/已封/未验/已弃。
public enum AccountStatus {
    ACTIVE, BANNED, UNVERIFIED, ABANDONED
}
