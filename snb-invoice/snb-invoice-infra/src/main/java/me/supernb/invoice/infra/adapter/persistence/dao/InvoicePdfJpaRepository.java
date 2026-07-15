package me.supernb.invoice.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoicePdfEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 发票 PDF DAO(request_id 1:1)。
public interface InvoicePdfJpaRepository extends JpaRepository<InvoicePdfEntity, Long> {

    Optional<InvoicePdfEntity> findByRequestId(long requestId);
}
