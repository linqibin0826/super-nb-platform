package me.supernb.activity.adapter.rest.request;

/// 报名请求体:campaignId 为雪花 id 字符串(对外 JSON id 一律字符串的家族惯例)。
public record RaffleEnterRequest(String campaignId) {}
