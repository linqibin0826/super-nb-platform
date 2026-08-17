package me.supernb.ops.domain.model;

/// 订阅状态:FREE 未付费/生效中/已到期/已取消/已封(BANNED 必须带封号时间,校验在 app 层)。
public enum SubStatus {
    FREE, ACTIVE, EXPIRED, CANCELED, BANNED
}
