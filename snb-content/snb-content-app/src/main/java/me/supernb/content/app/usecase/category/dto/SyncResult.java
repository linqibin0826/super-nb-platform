package me.supernb.content.app.usecase.category.dto;

/// 分类同步结果：upsert 条数 + 删除条数。
public record SyncResult(int upserted, int deleted) {
}
