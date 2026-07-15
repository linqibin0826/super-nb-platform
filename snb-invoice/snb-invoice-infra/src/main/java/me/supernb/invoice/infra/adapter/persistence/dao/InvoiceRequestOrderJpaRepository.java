package me.supernb.invoice.infra.adapter.persistence.dao;

import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceRequestOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// 订单占用行 DAO。
public interface InvoiceRequestOrderJpaRepository extends JpaRepository<InvoiceRequestOrderEntity, Long> {

    /// 释放某申请的全部占用(驳回/撤回同事务调用)。
    @Modifying
    @Query("UPDATE InvoiceRequestOrderEntity o SET o.active=false WHERE o.requestId=:requestId AND o.active=true")
    int releaseByRequest(@Param("requestId") long requestId);
}
