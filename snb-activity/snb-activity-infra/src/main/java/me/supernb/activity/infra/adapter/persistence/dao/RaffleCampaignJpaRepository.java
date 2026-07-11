package me.supernb.activity.infra.adapter.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleCampaignEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// `activity.raffle_campaign` 仓储。
public interface RaffleCampaignJpaRepository extends JpaRepository<RaffleCampaignEntity, Long> {

    /// 当前展示期:最新一期非 cancelled——active 报名/倒计时,drawn 停留展示开奖结果
    /// (迟到访客完整重放),直到下一期开放把它顶下去。
    Optional<RaffleCampaignEntity> findFirstByStatusNotOrderByEntryOpenAtDesc(String status);

    /// 到点待开奖:status=active 且 draw_at <= now(开奖任务每分钟轮询用,吃 idx_raffle_campaign_due)。
    List<RaffleCampaignEntity> findByStatusAndDrawAtLessThanEqual(String status, Instant now);

    /// 历届已开奖,按开奖时刻倒序取 limit 期。
    List<RaffleCampaignEntity> findByStatusOrderByDrawnAtDesc(String status, Limit limit);

    /// CAS 抢闸:只有 active→drawn 这一次翻转能命中(返回 1),重复/并发触发一律 0。
    /// drawn_at 用服务端 now() 写入,规避原生 SQL 绑定 Instant 的驱动差异。
    @Modifying
    @Query(value = "UPDATE activity.raffle_campaign SET status = 'drawn', drawn_at = now() "
            + "WHERE id = :id AND status = 'active'", nativeQuery = true)
    int markDrawn(@Param("id") long id);

    /// 开奖留痕:报名数与复核取消数(与 markDrawn 同事务内执行)。
    @Modifying
    @Query(value = "UPDATE activity.raffle_campaign SET entrant_count_at_draw = :entrants, "
            + "disqualified_count = :disqualified WHERE id = :id", nativeQuery = true)
    int recordDrawStats(@Param("id") long id, @Param("entrants") int entrants,
            @Param("disqualified") int disqualified);
}
