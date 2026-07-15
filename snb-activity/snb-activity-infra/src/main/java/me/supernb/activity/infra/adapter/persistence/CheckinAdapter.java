package me.supernb.activity.infra.adapter.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.infra.adapter.persistence.dao.CheckinRecordJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinRecordEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// CheckinPort 实现:写路径用原生 `INSERT ... ON CONFLICT DO NOTHING RETURNING id`
/// (spec §3.2,红二2——两步"先查后插"在弱网重试/双击下会撞唯一约束抛异常;单条原子语句从根源消除该窗口,
/// 并发下天然只有一行真正插入,其余全部幂等回放,不抛异常)。这是本仓库首次为一个新聚合选择
/// JdbcTemplate 原生写而非 JPA 实体 save() 路径——GateAdapter 的"先查后插+异常捕获"模板并不满足
/// spec 这条红线,直接抄那个模板反而不达标;JdbcTemplate 与注入的 JPA 仓库在同一个 `@Repository`
/// 内配合使用,读查询仍走 Spring Data 派生方法。
@Repository
public class CheckinAdapter implements CheckinPort {

    private final CheckinRecordJpaRepository records;
    private final JdbcTemplate jdbc;

    /// 构造:注入签到记录仓库与 Boot 主数据源的 JdbcTemplate。
    public CheckinAdapter(CheckinRecordJpaRepository records, JdbcTemplate jdbc) {
        this.records = records;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public CheckinOutcome checkIn(long userId, LocalDate day, Instant now, int nbPoints) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.checkin_record (id, user_id, checkin_date, checked_in_at) "
                        + "VALUES (?, ?, ?, ?) ON CONFLICT (user_id, checkin_date) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, day, Timestamp.from(now));
        if (!inserted.isEmpty()) {
            // 打卡即进账(同一事务):账本行 id 复用打卡行 id,source_ref=ISO 日期(与 V11 补铸同口径)
            if (nbPoints > 0) {
                jdbc.update("INSERT INTO activity.nb_ledger "
                                + "(id, user_id, entry_type, source_type, source_ref, points, occurred_at) "
                                + "VALUES (?, ?, 'EARN', 'checkin_daily', ?, ?, ?) "
                                + "ON CONFLICT (user_id, source_type, source_ref) DO NOTHING",
                        inserted.get(0), userId, day.toString(), nbPoints, Timestamp.from(now));
            }
            return new CheckinOutcome(true, day, now);
        }
        // 未真插入:当日已有记录(并发对手抢先或本人重复请求),幂等回放既有时刻
        Instant existing = records.findByUserIdAndCheckinDate(userId, day)
                .map(CheckinRecordEntity::getCheckedInAt)
                .orElse(now);
        return new CheckinOutcome(false, day, existing);
    }

    @Override
    public boolean checkedInOn(long userId, LocalDate day) {
        return records.findByUserIdAndCheckinDate(userId, day).isPresent();
    }

    @Override
    public int countInRange(long userId, LocalDate fromInclusive, LocalDate toInclusive) {
        return records.countByUserIdAndCheckinDateBetween(userId, fromInclusive, toInclusive);
    }

    @Override
    public int totalCheckins(long userId) {
        return records.countByUserId(userId);
    }

    @Override
    public List<LocalDate> datesInRange(long userId, LocalDate fromInclusive, LocalDate toInclusive) {
        return records.findByUserIdAndCheckinDateBetweenOrderByCheckinDateDesc(userId, fromInclusive, toInclusive)
                .stream()
                .map(CheckinRecordEntity::getCheckinDate)
                .toList();
    }

    @Override
    public List<Long> fullAttendanceUserIds(LocalDate fromInclusive, LocalDate toInclusive, long expectedDays) {
        return records.findFullAttendanceUserIds(fromInclusive, toInclusive, expectedDays);
    }
}
