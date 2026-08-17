package me.supernb.ops.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.infra.adapter.persistence.dao.OpsAccountJpaRepository;
import me.supernb.ops.infra.adapter.persistence.entity.OpsAccountEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// [OpsAccountRepository] 实现:email 唯一约束冲突映射 409(ops.account 唯一约束只有 email)。
@Repository
public class OpsAccountRepositoryAdapter implements OpsAccountRepository {

    private final OpsAccountJpaRepository dao;
    private final TransactionTemplate txTemplate;

    /// 构造:注入 DAO,事务管理器包成 TransactionTemplate。
    public OpsAccountRepositoryAdapter(OpsAccountJpaRepository dao, PlatformTransactionManager txManager) {
        this.dao = dao;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public long create(AccountData data) {
        try {
            return txTemplate.execute(status -> dao.save(new OpsAccountEntity(data)).getId());
        } catch (DataIntegrityViolationException e) {
            throw OpsException.duplicateEmail(data.email());
        }
    }

    @Override
    public boolean update(long id, AccountData data) {
        try {
            return Boolean.TRUE.equals(txTemplate.execute(status -> dao.findById(id)
                    .map(e -> {
                        e.apply(data);
                        dao.save(e);
                        return true;
                    })
                    .orElse(false)));
        } catch (DataIntegrityViolationException e) {
            throw OpsException.duplicateEmail(data.email());
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
    public Optional<AccountRow> find(long id) {
        return dao.findById(id).map(OpsAccountRepositoryAdapter::toRow);
    }

    @Override
    public List<AccountRow> listAll() {
        return dao.findAllByOrderByCreatedAtDesc().stream().map(OpsAccountRepositoryAdapter::toRow).toList();
    }

    private static AccountRow toRow(OpsAccountEntity e) {
        return new AccountRow(e.getId(), e.toData(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
