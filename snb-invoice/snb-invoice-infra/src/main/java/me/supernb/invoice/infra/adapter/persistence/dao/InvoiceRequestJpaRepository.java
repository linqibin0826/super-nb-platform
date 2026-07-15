package me.supernb.invoice.infra.adapter.persistence.dao;

import java.time.Instant;
import java.util.Collection;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// 申请单 DAO。状态转移全部守卫式 UPDATE(返回受影响行数,0=守卫未命中);
/// 守卫即并发控制,bulk UPDATE 不走 @Version 是刻意取舍。
public interface InvoiceRequestJpaRepository extends JpaRepository<InvoiceRequestEntity, Long> {

    @Modifying
    @Query("UPDATE InvoiceRequestEntity r SET r.status='INVOICING', r.feeChargedAt=:now "
            + "WHERE r.id=:id AND r.status='PENDING'")
    int markInvoicing(@Param("id") long id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE InvoiceRequestEntity r SET r.status='REJECTED', r.rejectReason=:reason "
            + "WHERE r.id=:id AND r.status IN :from")
    int reject(@Param("id") long id, @Param("reason") String reason, @Param("from") Collection<String> from);

    @Modifying
    @Query("UPDATE InvoiceRequestEntity r SET r.status='CANCELLED' "
            + "WHERE r.id=:id AND r.userId=:userId AND r.status='PENDING'")
    int cancel(@Param("id") long id, @Param("userId") long userId);

    @Modifying
    @Query("UPDATE InvoiceRequestEntity r SET r.status='ISSUED', r.issuedAt=:now "
            + "WHERE r.id=:id AND r.status='INVOICING'")
    int markIssued(@Param("id") long id, @Param("now") Instant now);
}
