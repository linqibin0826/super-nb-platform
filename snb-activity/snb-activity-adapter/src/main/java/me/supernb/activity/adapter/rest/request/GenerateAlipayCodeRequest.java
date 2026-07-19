package me.supernb.activity.adapter.rest.request;

/// prizeId 声明成 String,不是 Long——不确定 Jackson 默认配置会不会把 JSON 字符串宽松转成
/// Long(仓库里没找到对这点的显式配置或先例),干脆在 controller 里手动 parse,别赌默认行为。
public record GenerateAlipayCodeRequest(String prizeId, String tier, String displayName, int sortOrder) {}
