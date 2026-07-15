package me.supernb.invoice.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;
import java.math.BigDecimal;
import me.supernb.invoice.domain.model.InvoiceStatus;

/// 开票领域异常:trait 由 commons 引擎映射 HTTP(404/403/409/422/429)。
public class InvoiceException extends DomainException {

    private InvoiceException(String message, StandardErrorTrait trait) {
        super(message, trait);
    }

    /// 申请不存在或不属于当前用户(对外统一 404,不泄露他人单据存在性)。
    public static InvoiceException requestNotFound(long id) {
        return new InvoiceException("发票申请不存在: " + id, StandardErrorTrait.NOT_FOUND);
    }

    /// 抬头不存在或不属于当前用户。
    public static InvoiceException profileNotFound(long id) {
        return new InvoiceException("开票抬头不存在: " + id, StandardErrorTrait.NOT_FOUND);
    }

    /// 发票 PDF 尚未上传。
    public static InvoiceException pdfNotFound(long requestId) {
        return new InvoiceException("发票文件不存在: " + requestId, StandardErrorTrait.NOT_FOUND);
    }

    /// 管理端点要求 admin 身份。
    public static InvoiceException adminRequired() {
        return new InvoiceException("需要管理员身份", StandardErrorTrait.FORBIDDEN);
    }

    /// 入参校验失败(422)。
    public static InvoiceException invalidInput(String detail) {
        return new InvoiceException("入参不合法: " + detail, StandardErrorTrait.RULE_VIOLATION);
    }

    /// 勾选合计不足最低开票线。
    public static InvoiceException belowMinimum(BigDecimal total) {
        return new InvoiceException("勾选订单合计 ¥" + total + " 未达最低开票线 ¥1000", StandardErrorTrait.RULE_VIOLATION);
    }

    /// 余额不足以支付手续费(申请时预检;受理扣费时上游还有负余额保护兜底)。
    public static InvoiceException insufficientBalance(BigDecimal balance, BigDecimal fee) {
        return new InvoiceException("余额 " + balance + " 不足以支付手续费 " + fee, StandardErrorTrait.RULE_VIOLATION);
    }

    /// 抬头数量达上限。
    public static InvoiceException profileLimitReached(int max) {
        return new InvoiceException("开票抬头数量已达上限 " + max + " 条", StandardErrorTrait.QUOTA_EXCEEDED);
    }

    /// 已有进行中的申请(数据库 partial unique 兜底后的语义化)。
    public static InvoiceException duplicateActiveRequest() {
        return new InvoiceException("已有进行中的发票申请,请先等它完结或撤回", StandardErrorTrait.CONFLICT);
    }

    /// 所选订单已被其他有效申请占用。
    public static InvoiceException ordersOccupied() {
        return new InvoiceException("所选订单中有已被其他申请占用的,请刷新后重选", StandardErrorTrait.CONFLICT);
    }

    /// 状态机违规(如对非 PENDING 扣费)。
    public static InvoiceException invalidState(String expected, InvoiceStatus actual) {
        return new InvoiceException("当前状态 " + actual + " 不允许该操作(要求 " + expected + ")", StandardErrorTrait.CONFLICT);
    }

    /// 手续费扣/退被上游拒绝(如余额不足),携带上游报文供管理员判断。
    public static InvoiceException settlementFailed(String message) {
        return new InvoiceException("手续费结算失败: " + message, StandardErrorTrait.CONFLICT);
    }

    /// 非法 PDF(魔数/大小)。
    public static InvoiceException invalidPdf(String detail) {
        return new InvoiceException("发票文件不合法: " + detail, StandardErrorTrait.RULE_VIOLATION);
    }
}
