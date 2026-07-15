package me.supernb.invoice.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.PdfData;

/// 发票 PDF,映射 `invoice.invoice_pdf`(request_id 唯一,1:1;重传覆盖走 apply)。
@Entity
@Table(name = "invoice_pdf", schema = "invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoicePdfEntity extends ChildJpaEntity {

    @Column(name = "request_id", nullable = false, unique = true)
    private long requestId;

    @Column(nullable = false)
    private byte[] bytes;

    @Column(nullable = false)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String sha256;

    /// 新建:雪花取号 + 内容落库。
    public InvoicePdfEntity(long requestId, PdfData data) {
        setId(SnowflakeIdGenerator.getId());
        this.requestId = requestId;
        apply(data);
    }

    /// 覆盖内容(ISSUED 后重传补救)。
    public void apply(PdfData data) {
        this.bytes = data.bytes();
        this.filename = data.filename();
        this.sizeBytes = data.bytes().length;
        this.sha256 = data.sha256();
    }
}
