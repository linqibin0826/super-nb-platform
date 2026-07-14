package me.supernb.activity.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import me.supernb.activity.adapter.rest.request.MarkAchievementsSeenRequest;
import me.supernb.activity.adapter.rest.request.RaffleEnterRequest;
import me.supernb.activity.adapter.rest.response.AchievementWallResponse;
import me.supernb.activity.adapter.rest.response.MarkAchievementsSeenResponse;
import me.supernb.activity.adapter.rest.response.DrawResponse;
import me.supernb.activity.adapter.rest.response.RaffleCurrentResponse;
import me.supernb.activity.adapter.rest.response.RaffleEnterResponse;
import me.supernb.activity.adapter.rest.response.RaffleHistoryResponse;
import me.supernb.activity.adapter.rest.response.RaffleMeResponse;
import me.supernb.activity.adapter.rest.response.RaffleResultResponse;
import me.supernb.activity.adapter.rest.response.RaffleWinsResponse;
import me.supernb.activity.adapter.rest.response.GateDrawResponse;
import me.supernb.activity.adapter.rest.response.CheckinRewardsResponse;
import me.supernb.activity.adapter.rest.response.CheckinStatusResponse;
import me.supernb.activity.adapter.rest.response.RegistryStatusResponse;
import me.supernb.activity.app.usecase.achievement.command.MarkAchievementsSeenCommand;
import me.supernb.activity.app.usecase.achievement.query.AchievementWallQueryService;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.checkin.command.CheckInCommand;
import me.supernb.activity.app.usecase.checkin.query.CheckinRewardQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinStatusQueryService;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.gate.command.PerformGateDrawCommand;
import me.supernb.activity.app.usecase.raffle.RaffleQueryService;
import me.supernb.activity.app.usecase.raffle.command.RegisterRaffleCommand;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.registry.query.RegistryStatusQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.model.read.MyDrawView;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.model.read.PublicDraw;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;
import me.supernb.activity.domain.model.read.ReferralStats;
import me.supernb.activity.domain.model.read.RechargeEntry;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.BoardView;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/// 活动中心 REST 入口,路径 `/activity/v1/*`。`leaderboard`/`recharges`/`pool`/`recent-draws`/`registry-status`
/// 与发布会的 `raffle/current`/`raffle/{id}/result`/`raffle/history` 公开免登录;
/// `status`/`draw`/`my-draws`/`usage-leaderboard`/`raffle/me`/`raffle/enter` 需要登录——`@CurrentUser` 由 sub2api
/// starter 的解析器完成 introspect 校验(要求 active 的 user 或 admin 账号,否则 401)。写操作组装命令
/// 经 `CommandBus` 派发,其余只读端点直接调用注入的查询用例。
@RestController
@RequestMapping("/activity/v1")
public class ActivityController {

    private final CommandBus commandBus;
    private final DrawStatusQueryService drawStatusQuery;
    private final LeaderboardQueryService leaderboardQuery;
    private final RecentRechargesQueryService recentRechargesQuery;
    private final PoolQueryService poolQuery;
    private final RecentDrawsQueryService recentDrawsQuery;
    private final MyDrawsQueryService myDrawsQuery;
    private final ReferralLeaderboardQueryService referralQuery;
    private final UsageLeaderboardQueryService usageLeaderboardQuery;
    private final RaffleQueryService raffleQuery;
    private final RegistryStatusQueryService registryStatusQuery;
    private final CheckinStatusQueryService checkinStatusQuery;
    private final CheckinRewardQueryService checkinRewardQuery;
    private final AchievementWallQueryService achievementWallQuery;

    /// 构造:注入 CommandBus 与十二个查询用例(抽奖状态、充值榜、充值流水、奖池、近期中奖、我的中奖记录、
    /// 拉新榜、用量榜、发布会、注册表状态、签到状态、我的补给发放记录)。
    public ActivityController(
            CommandBus commandBus,
            DrawStatusQueryService drawStatusQuery,
            LeaderboardQueryService leaderboardQuery,
            RecentRechargesQueryService recentRechargesQuery,
            PoolQueryService poolQuery,
            RecentDrawsQueryService recentDrawsQuery,
            MyDrawsQueryService myDrawsQuery,
            ReferralLeaderboardQueryService referralQuery,
            UsageLeaderboardQueryService usageLeaderboardQuery,
            RaffleQueryService raffleQuery,
            RegistryStatusQueryService registryStatusQuery,
            CheckinStatusQueryService checkinStatusQuery,
            CheckinRewardQueryService checkinRewardQuery,
            AchievementWallQueryService achievementWallQuery) {
        this.commandBus = commandBus;
        this.drawStatusQuery = drawStatusQuery;
        this.leaderboardQuery = leaderboardQuery;
        this.recentRechargesQuery = recentRechargesQuery;
        this.poolQuery = poolQuery;
        this.recentDrawsQuery = recentDrawsQuery;
        this.myDrawsQuery = myDrawsQuery;
        this.referralQuery = referralQuery;
        this.usageLeaderboardQuery = usageLeaderboardQuery;
        this.raffleQuery = raffleQuery;
        this.registryStatusQuery = registryStatusQuery;
        this.checkinStatusQuery = checkinStatusQuery;
        this.checkinRewardQuery = checkinRewardQuery;
        this.achievementWallQuery = achievementWallQuery;
    }

    /// 活动期充值榜 Top10(公开)。无进行中活动 → 空列表,不是异常。
    @GetMapping("/leaderboard")
    public List<LeaderEntry> leaderboard() {
        return leaderboardQuery.leaderboard();
    }

    /// 活动期最近充值动态 Top20(公开)。无进行中活动 → 空列表。
    @GetMapping("/recharges")
    public List<RechargeEntry> recharges() {
        return recentRechargesQuery.recentRecharges();
    }

    /// 奖池实况,按档位返回余量(公开)。无进行中活动 → 空列表。
    @GetMapping("/pool")
    public List<PoolTier> pool() {
        return poolQuery.pool();
    }

    /// 最近真实中奖信息流(公开),排除安慰奖,展示名是服务端脱敏后的邮箱。
    /// 无进行中活动 → 空列表;查不到邮箱的中奖行(如账号已注销)直接跳过,不吐半条数据。
    @GetMapping("/recent-draws")
    public List<PublicDraw> recentDraws() {
        return recentDrawsQuery.recentDraws();
    }

    /// 我的抽奖资格与剩余次数(需登录)。无进行中活动 → 404,不像上面几个只读端点那样降级成空列表。
    @GetMapping("/status")
    public DrawStatus status(@CurrentUser UserProfile user) {
        return drawStatusQuery.status(user.id());
    }

    /// 抽一次(需登录)。无进行中活动 → 404;已无剩余抽奖次数 → 409;并发超发防护在 CommandBus
    /// 派发到的 Handler 背后(advisory lock),这一层只做协议转换与结果映射。
    @PostMapping("/draw")
    public DrawResponse draw(@CurrentUser UserProfile user) {
        DrawResult r = commandBus.handle(new PerformDrawCommand(user.id()));
        return new DrawResponse(r.amount(), r.redeemCode(), r.consolation());
    }

    /// 一次批量抽奖(需登录):原子抽 min(剩余, 10) 次,逐张返回。无进行中活动 → 404;
    /// 已无剩余次数 → 409;并发超发防护在 Handler 背后(advisory lock)。端点无请求体、无次数入参。
    @PostMapping("/draw/all")
    public List<DrawResponse> drawAll(@CurrentUser UserProfile user) {
        List<DrawResult> results = commandBus.handle(new PerformDrawAllCommand(user.id()));
        return results.stream()
                .map(r -> new DrawResponse(r.amount(), r.redeemCode(), r.consolation()))
                .toList();
    }

    /// 我在本活动的中奖历史(需登录),含安慰奖,enrich 兑换码当前状态,面向本人不做脱敏。
    /// 无进行中活动 → 空列表。
    @GetMapping("/my-draws")
    public List<MyDrawView> myDraws(@CurrentUser UserProfile user) {
        return myDrawsQuery.myDraws(user.id());
    }

    /// 拉新充值榜 Top(配置榜长,公开):被邀请新用户充值总额按邀请人聚合,原始总额降序,name 已脱敏。
    @GetMapping("/referral/recharge-board")
    public List<ReferralRechargeEntry> referralRechargeBoard() {
        return referralQuery.rechargeBoard();
    }

    /// 拉新人数榜 Top(配置榜长,公开):曾开通新人组的被邀请人数按邀请人聚合,人数降序,name 已脱敏。
    @GetMapping("/referral/invite-board")
    public List<ReferralInviteEntry> referralInviteBoard() {
        return referralQuery.inviteBoard();
    }

    /// 拉新全场统计(公开):本期新人总数(活动窗口内注册的用户数)。
    @GetMapping("/referral/stats")
    public ReferralStats referralStats() {
        return referralQuery.stats();
    }

    /// 活动中心注册表状态(公开,活动中心页与各活动页状态徽章用)。零 payload:
    /// 只回 id/kind/status/时间窗,无人数/名单/金额/身份(spec 2026-07-12 §6,加字段须先改 spec)。
    @GetMapping("/registry-status")
    public RegistryStatusResponse registryStatus() {
        return RegistryStatusResponse.of(registryStatusQuery.status());
    }

    /// 金票闸机抽签(需登录):资格/限次/概率全在服务端;门槛外一律 {eligible:false},
    /// 前端表现与普通过闸零差异(隐藏福利,spec gate §4)。写操作经 CommandBus 派发。
    @PostMapping("/gate/draw")
    public GateDrawResponse gateDraw(@CurrentUser UserProfile user) {
        return GateDrawResponse.of(commandBus.handle(new PerformGateDrawCommand(user.id())));
    }

    /// 用量排行榜(Token/金额双榜,需登录)。period=day|week|month|all,metric=tokens|amount;
    /// 参数非法 → 400;缓存未预热 → 503(spec §12)。
    @GetMapping("/usage-leaderboard")
    public BoardView usageLeaderboard(@RequestParam String period, @RequestParam String metric,
            @CurrentUser UserProfile user) {
        BoardPeriod p = parsePeriod(period);
        BoardMetric m = parseMetric(metric);
        // 金额榜只开日/周(站长 2026-07-11 拍板):历史累计氪金=鲸鱼终身消费公开账本,
        // 敏感且头部永久固化;后端必须封死,只藏前端 tab 挡不住 devtools。
        if (m == BoardMetric.AMOUNT && (p == BoardPeriod.MONTH || p == BoardPeriod.ALL)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount board supports day/week only");
        }
        BoardView view = usageLeaderboardQuery.board(p, m, user.id());
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "board warming up");
        }
        return view;
    }

    /// 解析周期参数(小写枚举名);非法值 → 400。解析放 adapter 层:domain 枚举不得依赖 spring-web。
    private static BoardPeriod parsePeriod(String raw) {
        try {
            return BoardPeriod.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid period: " + raw);
        }
    }

    /// 解析指标参数(小写枚举名);非法值 → 400。
    private static BoardMetric parseMetric(String raw) {
        try {
            return BoardMetric.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid metric: " + raw);
        }
    }

    /// 发布会当前期(公开):serverNow 供前端倒计时对齐;无进行中 → campaign=null。
    @GetMapping("/raffle/current")
    public RaffleCurrentResponse raffleCurrent() {
        return RaffleCurrentResponse.of(Instant.now(), raffleQuery.current().orElse(null));
    }

    /// 发布会开奖结果(公开,红头文件数据源);未开奖/不存在 → 404。
    @GetMapping("/raffle/{campaignId}/result")
    public RaffleResultResponse raffleResult(@PathVariable String campaignId) {
        return RaffleResultResponse.of(raffleQuery.result(parseId(campaignId)));
    }

    /// 历届通报存档(公开,固定 20 期)。
    @GetMapping("/raffle/history")
    public List<RaffleHistoryResponse> raffleHistory() {
        return raffleQuery.history().stream().map(RaffleHistoryResponse::of).toList();
    }

    /// 公开中奖记录(公开):坐标=(已开奖期 id, 参会证号),只认开奖通报里公开过的坐标;
    /// 未中过奖/坐标无效 → 404(参与史不泄露)。
    @GetMapping("/raffle/wins")
    public RaffleWinsResponse raffleWins(@RequestParam String campaignId, @RequestParam String entryNo) {
        return RaffleWinsResponse.of(raffleQuery.personWins(parseId(campaignId), parseEntryNo(entryNo)));
    }

    /// 我的列席状态与奖品(需登录);payload 只在已开奖且本人中奖时出现。
    @GetMapping("/raffle/me")
    public RaffleMeResponse raffleMe(@RequestParam String campaignId, @CurrentUser UserProfile user) {
        return RaffleMeResponse.of(raffleQuery.me(parseId(campaignId), user.id()));
    }

    /// 申请列席(需登录):不在报名期 404;资质不足 409(带「还需 ¥XX」);重复报名幂等返回既有参会证。
    /// ip/ua 只作秋后清算留痕。XFF 取【末值】=Caddy 亲验的真实对端:取首值可被报名者伪造投毒取证,
    /// 与同包 RaffleRateLimitFilter 口径一致(2026-07-13 安全审计,runbook ai-relay deployment/31)。
    @PostMapping("/raffle/enter")
    public RaffleEnterResponse raffleEnter(@RequestBody RaffleEnterRequest body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @CurrentUser UserProfile user) {
        String clientIp = null;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            clientIp = parts[parts.length - 1].trim();
        }
        RaffleEntryTicket ticket = commandBus.handle(new RegisterRaffleCommand(
                parseId(body.campaignId()), user.id(), clientIp, userAgent));
        return new RaffleEnterResponse(ticket.entryNo(), ticket.already());
    }

    /// 解析参会证号;非法值 → 400。
    private static int parseEntryNo(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid entryNo: " + raw);
        }
    }

    /// 解析雪花 id 字符串(对外 JSON id 一律字符串的家族惯例);非法值 → 400。
    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid id: " + raw);
        }
    }

    /// 签到(需登录,spec §7.3):账龄门槛与幂等写入在 CheckInHandler/CheckinAdapter 完成,写操作
    /// 经 CommandBus 派发;成功后 200 响应体是与 GET /checkin/status 完全同形的完整状态快照
    /// (2026-07-14 控制器裁决,覆盖三字段响应草稿——三字段会在跨阈值打卡时造成里程碑徽标已
    /// achieved 而 statusText 停留旧值的自相矛盾,前端被契约禁止自算业务文案,详见
    /// fe-contract.md POST /checkin 节的审查裁决)。账龄不足/今日已打过卡分别经 CommandBus
    /// 抛出的领域异常映射 403/409(commons StandardErrorTrait,由全局错误处理器接管)。
    @PostMapping("/checkin")
    public CheckinStatusResponse checkIn(@CurrentUser UserProfile user) {
        commandBus.handle(new CheckInCommand(user.id()));
        return CheckinStatusResponse.of(checkinStatusQuery.status(user.id()));
    }

    /// 签到状态(需登录):月度格子/累计天数/streak/里程碑 5·10·20·满勤/补给三档进度预览。
    @GetMapping("/checkin/status")
    public CheckinStatusResponse checkinStatus(@CurrentUser UserProfile user) {
        return CheckinStatusResponse.of(checkinStatusQuery.status(user.id()));
    }

    /// 我的补给发放记录(需登录,仅本人,spec §7.3 脱敏红线;响应体 `{"grants":[...]}` 包裹)。
    @GetMapping("/checkin/rewards")
    public CheckinRewardsResponse checkinRewards(@CurrentUser UserProfile user) {
        return CheckinRewardsResponse.of(checkinRewardQuery.myRewards(user.id()));
    }

    /// 我的成就墙(需登录,仅本人;机密档案未解锁项服务端脱敏,name/condition 恒 null)。
    @GetMapping("/checkin/achievements")
    public AchievementWallResponse checkinAchievements(@CurrentUser UserProfile user) {
        return AchievementWallResponse.of(achievementWallQuery.wall(user.id()));
    }

    /// 标记成就已读(需登录;幂等,重复标记不报错,响应 acknowledged=实际新标记行数)。
    @PostMapping("/checkin/achievements/seen")
    public MarkAchievementsSeenResponse markAchievementsSeen(@CurrentUser UserProfile user,
            @RequestBody MarkAchievementsSeenRequest request) {
        int acknowledged = commandBus.handle(new MarkAchievementsSeenCommand(user.id(), request.codes()));
        return new MarkAchievementsSeenResponse(acknowledged);
    }
}
