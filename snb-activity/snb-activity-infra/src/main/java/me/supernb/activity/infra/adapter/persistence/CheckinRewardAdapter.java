package me.supernb.activity.infra.adapter.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import me.supernb.activity.domain.model.checkin.CheckinRewardCandidate;
import me.supernb.activity.domain.model.checkin.CheckinRewardView;
import me.supernb.activity.domain.port.checkin.CheckinRewardPort;
import me.supernb.activity.infra.adapter.persistence.dao.CheckinRewardGrantJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinRewardGrantEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// CheckinRewardPort 实现:占位声明用原生 INSERT ON CONFLICT DO NOTHING RETURNING id
/// (同 CheckinAdapter 的写路径理由——单条原子语句消除"先查后插"竞态窗口);
/// 状态回写走 JPA 实体 mutator + 事务内 dirty checking(findById 拿到的是受管实体,
/// 事务提交时自动 flush,不需要显式 save)。
@Repository
public class CheckinRewardAdapter implements CheckinRewardPort {

    private final CheckinRewardGrantJpaRepository grants;
    private final JdbcTemplate jdbc;

    /// 构造:注入发放台账仓库与 Boot 主数据源的 JdbcTemplate。
    public CheckinRewardAdapter(CheckinRewardGrantJpaRepository grants, JdbcTemplate jdbc) {
        this.grants = grants;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Optional<Long> claim(long userId, LocalDate grantMonth, String tier, long groupId, String notes) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.checkin_reward_grant "
                        + "(id, user_id, grant_month, tier, group_id, status, notes) "
                        + "VALUES (?, ?, ?, ?, ?, 'pending', ?) "
                        + "ON CONFLICT (user_id, grant_month) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, grantMonth, tier, groupId, notes);
        return inserted.isEmpty() ? Optional.empty() : Optional.of(inserted.get(0));
    }

    @Override
    public List<CheckinRewardCandidate> byStatus(String status) {
        return grants.findByStatus(status).stream()
                .map(e -> new CheckinRewardCandidate(e.getId(), e.getUserId(), e.getGrantMonth(), e.getTier(),
                        e.getGroupId(), e.getNotes(), e.getAttempts()))
                .toList();
    }

    @Override
    @Transactional
    public void markSuccess(long grantId) {
        grants.findById(grantId).ifPresent(CheckinRewardGrantEntity::markSuccess);
    }

    @Override
    @Transactional
    public void markFailed(long grantId, String error) {
        grants.findById(grantId).ifPresent(e -> e.markFailed(error));
    }

    @Override
    @Transactional
    public void markDeferred(long grantId) {
        grants.findById(grantId).ifPresent(CheckinRewardGrantEntity::markDeferred);
    }

    @Override
    public List<CheckinRewardView> myGrants(long userId) {
        return grants.findByUserIdOrderByGrantMonthDesc(userId).stream()
                .map(e -> new CheckinRewardView(e.getGrantMonth(), e.getTier(), e.getStatus()))
                .toList();
    }
}
