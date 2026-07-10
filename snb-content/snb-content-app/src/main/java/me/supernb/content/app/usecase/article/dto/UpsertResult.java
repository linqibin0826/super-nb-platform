package me.supernb.content.app.usecase.article.dto;

/// upsert 结果：id 已 String 化（M11 雪花身份约定），created 标识本次是否新建。
public record UpsertResult(String id, boolean created) {
}
