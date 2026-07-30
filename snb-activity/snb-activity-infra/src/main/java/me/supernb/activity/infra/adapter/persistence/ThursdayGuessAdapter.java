package me.supernb.activity.infra.adapter.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort;
import me.supernb.activity.infra.adapter.persistence.entity.ThursdayGuessEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/// ThursdayGuessPort 实现。事务边界收在 infra(家族约定,app 层不带事务注解)。
@Component
public class ThursdayGuessAdapter implements ThursdayGuessPort {

    private final ThursdayGuessJpaRepository repo;

    /// 构造:注入猜桶竞猜仓库。
    public ThursdayGuessAdapter(ThursdayGuessJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> myGuess(LocalDate session, long userId) {
        return repo.findBySessionDateAndUserId(session, userId).map(ThursdayGuessEntity::getGuess);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(LocalDate session) {
        return repo.countBySessionDate(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuessRecord> all(LocalDate session) {
        return repo.findBySessionDateOrderByCreatedAtAsc(session).stream()
                .map(e -> new GuessRecord(e.getUserId(), e.getGuess()))
                .toList();
    }

    /// 落一条;已有则返回旧值不覆盖。
    ///
    /// 两道防线都要:先查一次挡掉绝大多数重复(便宜),再用唯一键兜住并发双击——
    /// 两个请求同时通过前置检查时,第二个会撞 `uq_thursday_guess_user_session`,
    /// 此时**回读已存在的那条**返回,而不是把异常抛给用户(他明明猜成功了)。
    /// REQUIRES_NEW 是为了让撞键回滚掉的只是这一笔插入,不连累外层。
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int submitOnce(LocalDate session, long userId, int guess) {
        Optional<ThursdayGuessEntity> existing = repo.findBySessionDateAndUserId(session, userId);
        if (existing.isPresent()) {
            return existing.get().getGuess();
        }
        try {
            repo.saveAndFlush(new ThursdayGuessEntity(session, userId, guess));
            return guess;
        } catch (DataIntegrityViolationException race) {
            return repo.findBySessionDateAndUserId(session, userId)
                    .map(ThursdayGuessEntity::getGuess)
                    .orElseThrow(() -> race);
        }
    }
}
