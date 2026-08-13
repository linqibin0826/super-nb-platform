package me.supernb.activity.infra.adapter.persistence;

import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import me.supernb.activity.infra.adapter.persistence.dao.SchoolCardJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.SchoolCardEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// SchoolCardPort 实现:开卡用原生 `INSERT ... ON CONFLICT DO NOTHING RETURNING id`
/// (SchoolClaimAdapter 同款理由);重置扣减用 JDBC 条件 UPDATE 原子完成
/// (`resets_used < maxEarned` 谓词在库内判,并发双击只有一个扣到);升档走 JPA dirty checking。
@Repository
public class SchoolCardAdapter implements SchoolCardPort {

    private final SchoolCardJpaRepository cards;
    private final JdbcTemplate jdbc;

    /// 构造:注入卡仓库与 Boot 主数据源的 JdbcTemplate。
    public SchoolCardAdapter(SchoolCardJpaRepository cards, JdbcTemplate jdbc) {
        this.cards = cards;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SchoolCardRecord> find(long userId) {
        return cards.findByUserId(userId).map(SchoolCardAdapter::toRecord);
    }

    @Override
    @Transactional
    public Optional<SchoolCardRecord> insert(long userId, int tier, long subscriptionId) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.school_card (id, user_id, tier, subscription_id) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (user_id) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, tier, subscriptionId);
        if (inserted.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SchoolCardRecord(inserted.get(0), userId, tier, subscriptionId, 0));
    }

    @Override
    @Transactional
    public void upgrade(long id, int tier, long subscriptionId) {
        cards.findById(id).ifPresent(e -> e.upgrade(tier, subscriptionId));
    }

    @Override
    @Transactional
    public boolean consumeReset(long id, int maxEarned) {
        int updated = jdbc.update(
                "UPDATE activity.school_card SET resets_used = resets_used + 1, updated_at = now() "
                        + "WHERE id = ? AND resets_used < ?",
                id, maxEarned);
        return updated == 1;
    }

    @Override
    @Transactional
    public void refundReset(long id) {
        jdbc.update(
                "UPDATE activity.school_card SET resets_used = GREATEST(resets_used - 1, 0), updated_at = now() "
                        + "WHERE id = ?",
                id);
    }

    private static SchoolCardRecord toRecord(SchoolCardEntity e) {
        return new SchoolCardRecord(e.getId(), e.getUserId(), e.getTier(),
                e.getSubscriptionId() == null ? 0 : e.getSubscriptionId(), e.getResetsUsed());
    }
}
