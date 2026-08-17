package me.supernb.ops.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.infra.adapter.persistence.dao.OpsSubscriptionJpaRepository;
import me.supernb.ops.infra.adapter.persistence.entity.OpsSubscriptionEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// [OpsSubscriptionRepository] 实现:同账号同服务撞 ux_ops_subscription_account_service 映射 409
/// (外键违约不会走到这——app 层建订阅前已核账号存在)。
@Repository
public class OpsSubscriptionRepositoryAdapter implements OpsSubscriptionRepository {

    private final OpsSubscriptionJpaRepository dao;
    private final TransactionTemplate txTemplate;

    /// 构造:注入 DAO,事务管理器包成 TransactionTemplate。
    public OpsSubscriptionRepositoryAdapter(OpsSubscriptionJpaRepository dao, PlatformTransactionManager txManager) {
        this.dao = dao;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public long create(SubscriptionData data) {
        try {
            return txTemplate.execute(status -> dao.save(new OpsSubscriptionEntity(data)).getId());
        } catch (DataIntegrityViolationException e) {
            throw OpsException.duplicateSubscription(data.service().name());
        }
    }

    @Override
    public boolean update(long id, SubscriptionData data) {
        try {
            return Boolean.TRUE.equals(txTemplate.execute(status -> dao.findById(id)
                    .map(e -> {
                        e.apply(data);
                        dao.save(e);
                        return true;
                    })
                    .orElse(false)));
        } catch (DataIntegrityViolationException e) {
            throw OpsException.duplicateSubscription(data.service().name());
        }
    }

    @Override
    public boolean delete(long id) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> {
            if (!dao.existsById(id)) {
                return false;
            }
            dao.deleteById(id);
            return true;
        }));
    }

    @Override
    public Optional<SubscriptionRow> find(long id) {
        return dao.findById(id).map(OpsSubscriptionRepositoryAdapter::toRow);
    }

    @Override
    public List<SubscriptionRow> listByAccount(long accountId) {
        return dao.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(OpsSubscriptionRepositoryAdapter::toRow).toList();
    }

    @Override
    public List<SubscriptionRow> listAll() {
        return dao.findAllByOrderByCreatedAtDesc().stream().map(OpsSubscriptionRepositoryAdapter::toRow).toList();
    }

    @Override
    public int countByAccount(long accountId) {
        return dao.countByAccountId(accountId);
    }

    private static SubscriptionRow toRow(OpsSubscriptionEntity e) {
        return new SubscriptionRow(e.getId(), e.toData(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
