/// `CampaignPort`:活动期查询端口。按 domain/port/{function} 惯例独立成子包(命名模式同
/// draw/),由 infra 的 CampaignAdapter 实现——查活动库取当前进行中活动;domain/app 只
/// 依赖这个接口,不感知持久化细节。
package me.supernb.activity.domain.port.campaign;
