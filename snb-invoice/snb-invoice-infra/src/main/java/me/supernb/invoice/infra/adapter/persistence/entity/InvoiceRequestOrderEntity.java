package me.supernb.invoice.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.invoice.domain.model.read.OrderLine;

/// 申请-订单占用行,映射 `invoice.invoice_request_order`(ChildJpaEntity:active 会随驳回/撤回翻转)。
@Entity
@Table(name = "invoice_request_order", schema = "invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceRequestOrderEntity extends ChildJpaEntity {

    @Column(name = "request_id", nullable = false)
    private long requestId;

    @Column(name = "order_id", nullable = false)
    private long orderId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    /// true=占用中(申请 PENDING/INVOICING/ISSUED);驳回/撤回置 false 释放。
    @Column(nullable = false)
    private boolean active;

    /// 新建:雪花取号 + 订单快照,初始占用。
    public InvoiceRequestOrderEntity(long requestId, OrderLine line) {
        setId(SnowflakeIdGenerator.getId());
        this.requestId = requestId;
        this.orderId = line.orderId();
        this.orderNo = line.orderNo();
        this.amount = line.amount();
        this.completedAt = line.completedAt();
        this.active = true;
    }
}
