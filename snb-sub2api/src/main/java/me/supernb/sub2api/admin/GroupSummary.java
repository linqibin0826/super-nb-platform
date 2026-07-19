package me.supernb.sub2api.admin;

/// sub2api 分组摘要(下拉选择用最小面):只透出 id 与名称,其余上游字段按需再加。
public record GroupSummary(long id, String name) {}
