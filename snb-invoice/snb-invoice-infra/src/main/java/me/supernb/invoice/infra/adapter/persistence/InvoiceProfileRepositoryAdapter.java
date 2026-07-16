package me.supernb.invoice.infra.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.infra.adapter.persistence.dao.InvoiceProfileJpaRepository;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceProfileEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// [InvoiceProfileRepository] 实现:全部操作 (userId, id) 双键定位,天然归属隔离。
@Repository
public class InvoiceProfileRepositoryAdapter implements InvoiceProfileRepository {

    private final InvoiceProfileJpaRepository dao;
    private final TransactionTemplate txTemplate;

    /// 构造:注入 DAO,事务管理器包成 TransactionTemplate。
    public InvoiceProfileRepositoryAdapter(InvoiceProfileJpaRepository dao, PlatformTransactionManager txManager) {
        this.dao = dao;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public long create(long userId, ProfileData data, Instant verifiedAt) {
        return txTemplate.execute(status -> dao.save(new InvoiceProfileEntity(userId, data, verifiedAt)).getId());
    }

    @Override
    public boolean update(long userId, long profileId, ProfileData data, Instant verifiedAt) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> dao.findByIdAndUserId(profileId, userId)
                .map(e -> {
                    e.apply(data, verifiedAt);
                    dao.save(e);
                    return true;
                })
                .orElse(false)));
    }

    @Override
    public boolean delete(long userId, long profileId) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> dao.deleteByIdAndUserId(profileId, userId) > 0));
    }

    @Override
    public Optional<StoredProfile> find(long userId, long profileId) {
        return dao.findByIdAndUserId(profileId, userId).map(e -> new StoredProfile(new ProfileData(
                ProfileType.valueOf(e.getType()), e.getTitle(), e.getTaxNo(), e.getRegAddress(),
                e.getRegPhone(), e.getBankName(), e.getBankAccount()), e.getVerifiedAt()));
    }

    @Override
    public int countByUser(long userId) {
        return dao.countByUserId(userId);
    }
}
