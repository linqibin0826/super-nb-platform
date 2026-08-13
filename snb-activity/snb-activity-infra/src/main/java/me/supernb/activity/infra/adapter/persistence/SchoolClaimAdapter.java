package me.supernb.activity.infra.adapter.persistence;

import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import me.supernb.activity.infra.adapter.persistence.dao.SchoolClaimJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.SchoolClaimEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// SchoolClaimPort 实现:占位用原生 `INSERT ... ON CONFLICT DO NOTHING RETURNING id`
/// (同 CheckinDailyRewardAdapter 的写路径理由——两步"先查后插"在双击/并发领取时会撞
/// 唯一约束抛异常,单条原子语句从根源消除该窗口);状态回写走 JPA 受管实体 dirty checking。
@Repository
public class SchoolClaimAdapter implements SchoolClaimPort {

    private final SchoolClaimJpaRepository claims;
    private final JdbcTemplate jdbc;

    /// 构造:注入台账仓库与 Boot 主数据源的 JdbcTemplate。
    public SchoolClaimAdapter(SchoolClaimJpaRepository claims, JdbcTemplate jdbc) {
        this.claims = claims;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SchoolClaimRecord> find(long userId, String kind, int tier) {
        return claims.findByUserIdAndKindAndTier(userId, kind, tier).map(SchoolClaimAdapter::toRecord);
    }

    @Override
    public List<SchoolClaimRecord> findByUser(long userId) {
        return claims.findByUserId(userId).stream().map(SchoolClaimAdapter::toRecord).toList();
    }

    @Override
    @Transactional
    public Optional<SchoolClaimRecord> insertPending(long userId, String kind, int tier, long groupId) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.school_claim (id, user_id, kind, tier, group_id, grant_status) "
                        + "VALUES (?, ?, ?, ?, ?, 'pending') "
                        + "ON CONFLICT (user_id, kind, tier) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, kind, tier, groupId);
        if (inserted.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SchoolClaimRecord(inserted.get(0), userId, kind, tier, groupId,
                SchoolClaimRecord.STATUS_PENDING, 0, null));
    }

    @Override
    @Transactional
    public void markSuccess(long id) {
        claims.findById(id).ifPresent(SchoolClaimEntity::markSuccess);
    }

    @Override
    @Transactional
    public void markFailed(long id, String error) {
        claims.findById(id).ifPresent(e -> e.markFailed(error));
    }

    private static SchoolClaimRecord toRecord(SchoolClaimEntity e) {
        return new SchoolClaimRecord(e.getId(), e.getUserId(), e.getKind(), e.getTier(),
                e.getGroupId(), e.getGrantStatus(), e.getAttempts(), e.getLastError());
    }
}
