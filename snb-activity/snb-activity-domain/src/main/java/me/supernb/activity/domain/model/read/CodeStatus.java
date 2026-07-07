package me.supernb.activity.domain.model.read;

import java.time.Instant;

/// 兑换码状态(读侧形态)。
public record CodeStatus(String status, Instant expiresAt) {
}
