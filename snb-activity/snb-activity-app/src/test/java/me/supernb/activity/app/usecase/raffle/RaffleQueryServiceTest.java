package me.supernb.activity.app.usecase.raffle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RaffleEntrant;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.model.read.raffle.MyRaffleView;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;
import me.supernb.activity.domain.model.read.raffle.RaffleCurrentView;
import me.supernb.activity.domain.model.read.raffle.RaffleResultView;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleEntryPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import org.junit.jupiter.api.Test;

/// 查询装配:议程单聚合(公开视图类型上没有 payload)、结果名单映射、me 的领奖闸门。
class RaffleQueryServiceTest {

    private final RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
    private final RaffleEntryPort entryPort = mock(RaffleEntryPort.class);
    private final RafflePrizePort prizePort = mock(RafflePrizePort.class);
    private final RaffleGateReadPort gatePort = mock(RaffleGateReadPort.class);
    private final RaffleQueryService svc =
            new RaffleQueryService(campaignPort, entryPort, prizePort, gatePort);

    private static final Instant OPEN = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant CLOSE = Instant.parse("2026-07-13T02:00:00Z");

    private static RaffleCampaign active() {
        return new RaffleCampaign(1, "第一届发布会", OPEN, CLOSE, CLOSE, GateType.RECHARGE,
                new BigDecimal("100"), OPEN, null, WeightMode.EQUAL, "active", null, null, null);
    }

    private static RaffleCampaign drawn() {
        return new RaffleCampaign(1, "第一届发布会", OPEN, CLOSE, CLOSE, GateType.RECHARGE,
                new BigDecimal("100"), OPEN, null, WeightMode.EQUAL, "drawn",
                Instant.parse("2026-07-13T02:30:00Z"), 3, 1);
    }

    @Test
    void currentAggregatesPrizeBillAndEntrants() {
        when(campaignPort.current()).thenReturn(Optional.of(active()));
        when(entryPort.count(1)).thenReturn(128);
        when(entryPort.recent(1, 12)).thenReturn(List.of(new RaffleEntrant(42, 37)));
        when(gatePort.displayNames(any())).thenReturn(Map.of(42L, "12***67@qq.com"));
        when(prizePort.byCampaign(1)).thenReturn(List.of(
                new RafflePrize(1001, "S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", "FAKE-KFC", 0, null, null),
                new RafflePrize(1002, "C", "碳酸饮料民生保障计划", "ALIPAY_CODE", "FAKE-COLA-1", 1, null, null),
                new RafflePrize(1003, "C", "碳酸饮料民生保障计划", "ALIPAY_CODE", "FAKE-COLA-2", 2, null, null)));
        RaffleCurrentView v = svc.current().orElseThrow();
        assertThat(v.entrantCount()).isEqualTo(128);
        assertThat(v.recentEntrants()).containsExactly(new RaffleCurrentView.Entrant(37, "12***67@qq.com"));
        assertThat(v.prizes()).containsExactly(
                new RaffleCurrentView.PrizeLine("S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", 1),
                new RaffleCurrentView.PrizeLine("C", "碳酸饮料民生保障计划", "ALIPAY_CODE", 2)); // 同名聚合计数
    }

    @Test
    void resultRequiresDrawnAndMapsWinnersOnly() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(drawn()));
        when(prizePort.byCampaign(1)).thenReturn(List.of(
                new RafflePrize(1001, "S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", "FAKE-KFC", 0, 42L,
                        Instant.parse("2026-07-13T02:30:00Z")),
                new RafflePrize(1002, "C", "碳酸饮料民生保障计划", "ALIPAY_CODE", "FAKE-COLA", 1, null, null)));
        when(entryPort.entrants(1)).thenReturn(List.of(new RaffleEntrant(42, 37)));
        when(gatePort.displayNames(any())).thenReturn(Map.of(42L, "老王"));
        RaffleResultView v = svc.result(1);
        assertThat(v.winners()).containsExactly(
                new RaffleResultView.Winner(37, "老王", "S", "疯狂星期四专项(V我50)")); // 流拍件不出名单
        assertThat(v.entrantCountAtDraw()).isEqualTo(3);
        assertThat(v.disqualifiedCount()).isEqualTo(1);
    }

    @Test
    void resultBeforeDrawRejected() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(active()));
        assertThatThrownBy(() -> svc.result(1)).isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void mePayloadOnlyAfterDrawnForWinner() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(drawn()));
        when(entryPort.find(1, 42)).thenReturn(Optional.of(new RaffleEntrant(42, 37)));
        when(gatePort.gateValue(anyLong(), any(), any(), any())).thenReturn(new BigDecimal("150"));
        when(prizePort.wonBy(1, 42)).thenReturn(Optional.of(
                new RafflePrize(1001, "S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", "FAKE-KFC-50", 0, 42L,
                        Instant.parse("2026-07-13T02:30:00Z"))));
        MyRaffleView v = svc.me(1, 42);
        assertThat(v.entered()).isTrue();
        assertThat(v.entryNo()).isEqualTo(37);
        assertThat(v.eligible()).isTrue();
        assertThat(v.myPrize().payload()).isEqualTo("FAKE-KFC-50"); // 全系统唯一放行 payload 的路径
    }

    @Test
    void meBeforeDrawNeverQueriesPrize() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(active()));
        when(entryPort.find(1, 42)).thenReturn(Optional.of(new RaffleEntrant(42, 37)));
        when(gatePort.gateValue(anyLong(), any(), any(), any())).thenReturn(new BigDecimal("150"));
        MyRaffleView v = svc.me(1, 42);
        assertThat(v.myPrize()).isNull(); // 未开奖不查不吐(抢跑防护)
    }

    @Test
    void personWinsAggregatesAcrossDrawnCampaigns() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(drawn()));
        when(entryPort.findByNo(1, 37)).thenReturn(Optional.of(new RaffleEntrant(42, 37)));
        when(prizePort.winsOf(42)).thenReturn(List.of(
                new PersonWinsView.Win(1, "第一届发布会", Instant.parse("2026-07-13T02:30:00Z"),
                        "S", "疯狂星期四专项(V我50)"),
                new PersonWinsView.Win(9, "第〇届彩排", Instant.parse("2026-07-04T02:30:00Z"),
                        "B", "瑞幸咖啡(9.9)")));
        when(gatePort.displayNames(List.of(42L))).thenReturn(Map.of(42L, "12***67@qq.com"));
        PersonWinsView v = svc.personWins(1, 37);
        assertThat(v.displayName()).isEqualTo("12***67@qq.com");
        assertThat(v.wins()).hasSize(2);
        assertThat(v.wins().get(0).tier()).isEqualTo("S");
    }

    @Test
    void personWinsRejectsUndrawnCoordinate() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(active()));
        assertThatThrownBy(() -> svc.personWins(1, 37)).isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void personWinsHidesNonWinnersAndUnknownCoordinates() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(drawn()));
        // 参与了但没中过奖:404,参与史不泄露
        when(entryPort.findByNo(1, 37)).thenReturn(Optional.of(new RaffleEntrant(42, 37)));
        when(prizePort.winsOf(42)).thenReturn(List.of());
        assertThatThrownBy(() -> svc.personWins(1, 37)).isInstanceOf(RaffleNotFoundException.class);
        // 坐标不存在:404
        when(entryPort.findByNo(1, 99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc.personWins(1, 99)).isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void personWinsRejectsWinnerOfOtherCampaignOnly() {
        // 本期(1)报名但没中奖、只在别期(9)中过奖:必须 404,不得穿透泄露跨期中奖史
        // (2026-07-13 安全审计:仅 winsOf 非空不够,须命中本期 campaignId)
        when(campaignPort.byId(1)).thenReturn(Optional.of(drawn()));
        when(entryPort.findByNo(1, 50)).thenReturn(Optional.of(new RaffleEntrant(77, 50)));
        when(prizePort.winsOf(77)).thenReturn(List.of(
                new PersonWinsView.Win(9, "第〇届彩排", Instant.parse("2026-07-04T02:30:00Z"),
                        "B", "瑞幸咖啡(9.9)")));
        assertThatThrownBy(() -> svc.personWins(1, 50)).isInstanceOf(RaffleNotFoundException.class);
    }
}
