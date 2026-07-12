package me.supernb.activity.adapter.rest.response;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.registry.RegistryEntryStatus;

/// 活动中心注册表状态响应:字段白名单 id/kind/status/startsAt/endsAt(spec 2026-07-12 §6,零 payload)。
public record RegistryStatusResponse(List<EntryView> campaigns) {

    public static RegistryStatusResponse of(List<RegistryEntryStatus> entries) {
        return new RegistryStatusResponse(entries.stream()
                .map(e -> new EntryView(e.id(), e.kind(), e.status(), e.startsAt(), e.endsAt()))
                .toList());
    }

    public record EntryView(String id, String kind, String status, Instant startsAt, Instant endsAt) {}
}
