package me.supernb.invoice.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 抬头 DAO(归属双键定位)。
public interface InvoiceProfileJpaRepository extends JpaRepository<InvoiceProfileEntity, Long> {

    Optional<InvoiceProfileEntity> findByIdAndUserId(long id, long userId);

    int countByUserId(long userId);

    long deleteByIdAndUserId(long id, long userId);
}
