package me.supernb.activity.domain.model.raffle;

/// 参与门槛指标:RECHARGE=累计充值(payment_orders 真金口径),SPEND=累计消费(usage_logs 余额扣费口径)。
public enum GateType { RECHARGE, SPEND }
