package me.supernb.activity.app.usecase.thursday.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.app.usecase.thursday.GuessSettlement;
import me.supernb.activity.app.usecase.thursday.HiddenBucketDraw;
import me.supernb.activity.app.usecase.thursday.ThursdayBucketView;
import me.supernb.activity.app.usecase.thursday.ThursdayGuessView;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.domain.port.read.GateRechargeReadPort;
import me.supernb.activity.domain.port.read.ThursdayBucketReadPort;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort.GuessRecord;
import org.springframework.stereotype.Service;

/// 疯四桶状态查询(spec §3)。资格与桶序全在服务端算,时区显式 Asia/Shanghai——
/// 前端不得用 `new Date()` 自己推"今天是不是周四"(签到 spec §7.3 同款纪律:
/// 客户端时钟不可信,用户改系统时间就能骗出一个不存在的场次)。
///
/// 领取命令与本查询共用这一份判定,避免两处口径漂移。
@Service
public class ThursdayBucketQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ThursdayProperties props;
    private final ThursdayBucketReadPort readPort;
    private final ThursdayGuessPort guessPort;
    private final GateRechargeReadPort rechargePort;

    /// 构造:注入疯四场次配置、只读端口、竞猜存取端口,以及累计充值读端口
    /// (猜桶门槛与金票闸机同口径,复用同一个端口,别再造第二套「够不够 ¥30」的判定)。
    public ThursdayBucketQueryService(ThursdayProperties props, ThursdayBucketReadPort readPort,
            ThursdayGuessPort guessPort, GateRechargeReadPort rechargePort) {
        this.props = props;
        this.readPort = readPort;
        this.guessPort = guessPort;
        this.rechargePort = rechargePort;
    }

    /// 本人在今天这一场的猜桶视图;非场次日返回休眠态。
    public ThursdayGuessView guessView(long userId) {
        return guessView(userId, Instant.now());
    }

    /// 同上,`now` 显式传入以便单测钉住「封猜前/封猜后/结算后」三条分支。
    public ThursdayGuessView guessView(long userId, Instant now) {
        LocalDate today = LocalDate.now(ZONE);
        if (props.groupIdFor(today) == null) {
            return ThursdayGuessView.closed(props.guessThresholdCny());
        }
        Instant closeAt = today.atTime(props.guessCloseAt()).atZone(ZONE).toInstant();
        boolean eligible = rechargePort.totalRecharged(userId)
                .compareTo(props.guessThresholdCny()) >= 0;
        Integer mine = guessPort.myGuess(today, userId).orElse(null);
        long count = guessPort.count(today);

        List<Integer> hidden = hiddenBuckets(today, now);
        if (hidden == null) {
            // 还没到开奖时刻:不结算,也不泄露答案。
            return new ThursdayGuessView(eligible, now.isBefore(closeAt), mine, count, closeAt,
                    props.guessThresholdCny(), null, null, false);
        }
        int answer = frozenBucketCount(today);
        var winner = GuessSettlement.winner(guessPort.all(today), answer);
        return new ThursdayGuessView(eligible, false, mine, count, closeAt, props.guessThresholdCny(),
                answer, winner.map(GuessRecord::guess).orElse(null),
                winner.filter(w -> w.userId() == userId).isPresent());
    }

    /// 能不能收这一笔猜测(场次内 + 够门槛 + 未封猜);提交命令用。
    public boolean guessAcceptable(long userId, Instant now) {
        LocalDate today = LocalDate.now(ZONE);
        if (props.groupIdFor(today) == null) {
            return false;
        }
        if (!now.isBefore(today.atTime(props.guessCloseAt()).atZone(ZONE).toInstant())) {
            return false;
        }
        return rechargePort.totalRecharged(userId).compareTo(props.guessThresholdCny()) >= 0;
    }

    /// 本人在今天这一场的视图;今天不是场次(或未配分组)返回休眠态。
    public ThursdayBucketView view(long userId) {
        LocalDate today = LocalDate.now(ZONE);
        Long groupId = props.groupIdFor(today);
        if (groupId == null) {
            return ThursdayBucketView.closed(props.bucketLimit());
        }
        List<Long> qualified = qualifiedToday(today);
        int idx = qualified.indexOf(userId);
        boolean eligible = idx >= 0;
        boolean claimed = eligible && readPort.alreadyClaimed(userId, groupId, props.notes());
        Integer bucketNo = eligible ? idx + 1 : null;
        List<Integer> hidden = hiddenBuckets(today);
        boolean hiddenWin = hidden != null && bucketNo != null && hidden.contains(bucketNo);
        return new ThursdayBucketView(true, eligible, claimed, bucketNo,
                qualified.size(), props.bucketLimit(), hidden, hiddenWin);
    }

    /// 隐藏款(瑞幸)中奖桶序;**未到开奖时刻返回 null**(前端据此显示"待开奖")。
    ///
    /// 抽签范围锁在「开奖时刻的桶数」——窗口是 [当天 00:00, 开奖时刻),不是整天。
    /// 开奖后这个数就不再变,所以号也不再变;开奖后才充值进来的人拿得到桶、但不参与本场隐藏款
    /// (与运营脚本 22:00 批扫的名单口径一致)。
    public List<Integer> hiddenBuckets(LocalDate day) {
        return hiddenBuckets(day, Instant.now());
    }

    /// 同上,`now` 显式传入——留这道缝是为了让"开奖前后"两条分支都能被单测钉住;
    /// 否则测试只能靠真实时钟,一到 22:00 就自己变红(时间型 flaky)。生产代码只走上面那个重载。
    public List<Integer> hiddenBuckets(LocalDate day, Instant now) {
        Instant reveal = day.atTime(props.revealAt()).atZone(ZONE).toInstant();
        if (now.isBefore(reveal)) {
            return null;
        }
        return HiddenBucketDraw.draw(props.hiddenSalt(), day, frozenBucketCount(day), props.hiddenCount());
    }

    /// 开奖时刻冻结下来的出桶数 —— 隐藏款摇号的范围,也是猜桶竞猜的标准答案。
    /// 两处必须用同一个数,否则页面会出现「隐藏款按 8 桶摇、竞猜按 9 桶判」这种自相矛盾。
    public int frozenBucketCount(LocalDate day) {
        Instant start = day.atStartOfDay(ZONE).toInstant();
        Instant reveal = day.atTime(props.revealAt()).atZone(ZONE).toInstant();
        return readPort.qualifiedInOrder(start, reveal, props.minAmountCny(), props.bucketLimit()).size();
    }

    /// 今天这一场的资格名单(到账顺序,已封顶)。
    public List<Long> qualifiedToday(LocalDate day) {
        Instant start = day.atStartOfDay(ZONE).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZONE).toInstant();
        return readPort.qualifiedInOrder(start, end, props.minAmountCny(), props.bucketLimit());
    }

    /// 今天的场次分组 id(领取命令用);非场次日为 null。
    public Long groupIdToday() {
        return props.groupIdFor(LocalDate.now(ZONE));
    }

    /// 今天(Asia/Shanghai)的日期——领取命令与本查询共用同一个"今天",防跨零点错场。
    public LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
