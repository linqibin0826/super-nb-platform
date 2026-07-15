package me.supernb.invoice.domain.port.settlement;

import java.math.BigDecimal;

/// 手续费结算端口:经 sub2api admin API 扣/退站内余额,永不直写库。
/// 上游明确拒绝(如余额不足)抛 InvoiceException.settlementFailed;传输层故障原样上抛(可重试)。
public interface FeeSettlementPort {

    /// 扣手续费。note 必须含 request_no(上游幂等键=端点+payload,TTL 2h 内同 note 重放不双扣)。
    void charge(long userId, BigDecimal amount, String note);

    /// 退手续费(INVOICING 驳回时可选)。
    void refund(long userId, BigDecimal amount, String note);
}
