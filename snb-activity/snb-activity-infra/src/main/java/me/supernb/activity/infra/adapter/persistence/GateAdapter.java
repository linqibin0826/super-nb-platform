package me.supernb.activity.infra.adapter.persistence;

import java.time.Instant;
import java.time.LocalDate;
import me.supernb.activity.domain.exception.GateAlreadyAttemptedTodayException;
import me.supernb.activity.domain.model.gate.GateDrawOutcome;
import me.supernb.activity.domain.port.gate.GatePort;
import me.supernb.activity.infra.adapter.persistence.entity.GateAttemptEntity;
import me.supernb.activity.infra.adapter.persistence.entity.GateTicketEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// 金票闸机事务体(事务边界收在 infra,家族约定):
/// 当日 attempt 回放 → (wantWin 且池非空)SKIP LOCKED 领码 → 落 attempt。
/// 并发双击:先查后插仍可能撞 `(user_id, attempt_date)` 唯一键 →
/// 抛 [GateAlreadyAttemptedTodayException](本事务整体回滚,已领的码一并回滚不漏池),
/// 由 handler 以 wantWin=false 换新事务降级重读。
@Repository
public class GateAdapter implements GatePort {

    private final GateTicketJpaRepository tickets;
    private final GateAttemptJpaRepository attempts;

    /// 构造:注入码池与抽签记录仓库。
    public GateAdapter(GateTicketJpaRepository tickets, GateAttemptJpaRepository attempts) {
        this.tickets = tickets;
        this.attempts = attempts;
    }

    @Override
    @Transactional
    public GateDrawOutcome drawFor(long userId, LocalDate day, boolean wantWin) {
        var existing = attempts.findByUserIdAndAttemptDate(userId, day);
        if (existing.isPresent()) {
            return replay(existing.get());
        }
        GateTicketEntity ticket = null;
        if (wantWin) {
            ticket = tickets.pickAvailableId().flatMap(tickets::findById).orElse(null); // 池空=未中
            if (ticket != null) {
                ticket.claim(userId, Instant.now());
            }
        }
        var attempt = new GateAttemptEntity(userId, day, ticket != null, ticket == null ? null : ticket.getId());
        try {
            attempts.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException race) {
            throw new GateAlreadyAttemptedTodayException();
        }
        return ticket == null ? GateDrawOutcome.lose()
                : new GateDrawOutcome(true, ticket.getAmount(), ticket.getCode(), ticket.getClaimedAt());
    }

    /// 当日既有记录回放:中过则返回同一张码(防丢码),未中即未中。
    private GateDrawOutcome replay(GateAttemptEntity a) {
        if (!a.isWon() || a.getTicketId() == null) {
            return GateDrawOutcome.lose();
        }
        return tickets.findById(a.getTicketId())
                .map(t -> new GateDrawOutcome(true, t.getAmount(), t.getCode(), t.getClaimedAt()))
                .orElseGet(GateDrawOutcome::lose);
    }
}
