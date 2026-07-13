package me.supernb.activity.app.usecase.raffle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RaffleEntrant;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.read.raffle.MyRaffleView;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;
import me.supernb.activity.domain.model.read.raffle.RaffleCurrentView;
import me.supernb.activity.domain.model.read.raffle.RaffleHistoryItem;
import me.supernb.activity.domain.model.read.raffle.RaffleResultView;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleEntryPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import org.springframework.stereotype.Service;

/// 发布会读装配:current/result/history 公开,me 登录本人。
/// 公开视图只搬运非机密字段——RaffleCurrentView/RaffleResultView 类型上没有 payload,想漏都漏不出去;
/// me 的奖品只在已开奖后查询(未开奖连查都不查,防抢跑)。
@Service
public class RaffleQueryService {

    private static final int HISTORY_LIMIT = 20;
    private static final int RECENT_ENTRANTS = 12;

    private final RaffleCampaignPort campaignPort;
    private final RaffleEntryPort entryPort;
    private final RafflePrizePort prizePort;
    private final RaffleGateReadPort gatePort;

    /// 构造:注入四个端口。
    public RaffleQueryService(RaffleCampaignPort campaignPort, RaffleEntryPort entryPort,
            RafflePrizePort prizePort, RaffleGateReadPort gatePort) {
        this.campaignPort = campaignPort;
        this.entryPort = entryPort;
        this.prizePort = prizePort;
        this.gatePort = gatePort;
    }

    /// 当前期公开视图;无进行中返回 empty(端点回 campaign=null,不是异常)。
    public Optional<RaffleCurrentView> current() {
        return campaignPort.current().map(c -> {
            List<RaffleEntrant> recent = entryPort.recent(c.id(), RECENT_ENTRANTS);
            Map<Long, String> names = gatePort.displayNames(
                    recent.stream().map(RaffleEntrant::userId).toList());
            List<RaffleCurrentView.Entrant> entrants = recent.stream()
                    .map(e -> new RaffleCurrentView.Entrant(e.entryNo(),
                            names.getOrDefault(e.userId(), "神秘代表")))
                    .toList();
            return new RaffleCurrentView(c.id(), c.name(), c.entryOpenAt(), c.entryCloseAt(), c.drawAt(),
                    c.gateType().name(), c.gateAmount(), c.gateFrom(), c.weightMode().name(), c.status(),
                    entryPort.count(c.id()), entrants, prizeBill(c.id()));
        });
    }

    /// 议程单:奖品按(tier, displayName, kind)聚合计数,保持 sort_order 出场顺序;payload 不搬运。
    private List<RaffleCurrentView.PrizeLine> prizeBill(long campaignId) {
        Map<String, RaffleCurrentView.PrizeLine> lines = new LinkedHashMap<>();
        for (RafflePrize p : prizePort.byCampaign(campaignId)) {
            String key = p.tier() + "|" + p.displayName() + "|" + p.kind();
            lines.merge(key, new RaffleCurrentView.PrizeLine(p.tier(), p.displayName(), p.kind(), 1),
                    (a, b) -> new RaffleCurrentView.PrizeLine(a.tier(), a.displayName(), a.kind(),
                            a.count() + 1));
        }
        return List.copyOf(lines.values());
    }

    /// 开奖结果公开视图;未开奖/不存在/已作废抛 RaffleNotFoundException(404)。
    public RaffleResultView result(long campaignId) {
        RaffleCampaign c = campaignPort.byId(campaignId)
                .filter(RaffleCampaign::drawn)
                .orElseThrow(RaffleNotFoundException::new);
        Map<Long, Integer> entryNos = entryPort.entrants(campaignId).stream()
                .collect(Collectors.toMap(RaffleEntrant::userId, RaffleEntrant::entryNo));
        List<RafflePrize> assigned = prizePort.byCampaign(campaignId).stream()
                .filter(RafflePrize::assigned)
                .toList();
        Map<Long, String> names = gatePort.displayNames(
                assigned.stream().map(RafflePrize::winnerUserId).toList());
        List<RaffleResultView.Winner> winners = assigned.stream()
                .map(p -> new RaffleResultView.Winner(
                        entryNos.getOrDefault(p.winnerUserId(), 0),
                        names.getOrDefault(p.winnerUserId(), "神秘代表"),
                        p.tier(), p.displayName()))
                .toList();
        return new RaffleResultView(c.id(), c.name(), c.drawnAt(),
                c.entrantCountAtDraw() == null ? 0 : c.entrantCountAtDraw(),
                c.disqualifiedCount() == null ? 0 : c.disqualifiedCount(), winners);
    }

    /// 历届通报存档(固定 20 期;期数少、逐期两查可接受)。
    public List<RaffleHistoryItem> history() {
        return campaignPort.drawnHistory(HISTORY_LIMIT).stream()
                .map(c -> new RaffleHistoryItem(c.id(), c.name(), c.drawnAt(),
                        prizePort.byCampaign(c.id()).size(), entryPort.count(c.id())))
                .toList();
    }

    /// 公开中奖记录:坐标=(已开奖期, 参会证号)——只认已在开奖通报里公开过的坐标,
    /// 不新增任何身份标识出口;坐标期未开奖、坐标不存在、或该人从未中过奖,一律 404(参与史不泄露)。
    public PersonWinsView personWins(long campaignId, int entryNo) {
        campaignPort.byId(campaignId)
                .filter(RaffleCampaign::drawn)
                .orElseThrow(RaffleNotFoundException::new);
        var entrant = entryPort.findByNo(campaignId, entryNo).orElseThrow(RaffleNotFoundException::new);
        List<PersonWinsView.Win> wins = prizePort.winsOf(entrant.userId());
        // 坐标必须是【本期】真实中奖者:winsOf 是跨期全量,仅非空不够。否则任意报名者(含未中奖者)的
        // 坐标都能穿透查出其在别期的中奖史,击穿本方法「参与史不泄露」承诺(2026-07-13 安全审计,
        // runbook ai-relay deployment/31)。仍复用已取列表原样返回(真中奖者的跨期荣誉墙是预期功能)。
        if (wins.stream().noneMatch(w -> w.campaignId() == campaignId)) {
            throw new RaffleNotFoundException();
        }
        String name = gatePort.displayNames(List.of(entrant.userId()))
                .getOrDefault(entrant.userId(), "神秘代表");
        return new PersonWinsView(name, wins);
    }

    /// 本人视图:资质窗口=[gate_from, min(now, entry_close_at))(截止后进度冻结);
    /// 奖品只在已开奖后查询。
    public MyRaffleView me(long campaignId, long userId) {
        RaffleCampaign c = campaignPort.byId(campaignId).orElseThrow(RaffleNotFoundException::new);
        Instant now = Instant.now();
        Instant to = now.isBefore(c.entryCloseAt()) ? now : c.entryCloseAt();
        BigDecimal value = gatePort.gateValue(userId, c.gateType(), c.gateFrom(), to);
        Optional<RaffleEntrant> entry = entryPort.find(campaignId, userId);
        MyRaffleView.MyPrize prize = null;
        if (c.drawn()) {
            prize = prizePort.wonBy(campaignId, userId)
                    .map(p -> new MyRaffleView.MyPrize(p.tier(), p.displayName(), p.kind(), p.payload()))
                    .orElse(null);
        }
        return new MyRaffleView(entry.isPresent(), entry.map(RaffleEntrant::entryNo).orElse(null),
                value, c.gateAmount(), value.compareTo(c.gateAmount()) >= 0, prize);
    }
}
