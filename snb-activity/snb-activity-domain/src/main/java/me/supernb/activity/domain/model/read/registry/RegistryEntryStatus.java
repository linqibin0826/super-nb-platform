package me.supernb.activity.domain.model.read.registry;

import java.time.Instant;

/// 活动中心注册表条目状态(读视图,零 payload):id 与活动中心页 registry.json 对齐,
/// status ∈ upcoming | running | ended;常驻类(evergreen)窗口为 null。
public record RegistryEntryStatus(String id, String kind, String status, Instant startsAt, Instant endsAt) {}
