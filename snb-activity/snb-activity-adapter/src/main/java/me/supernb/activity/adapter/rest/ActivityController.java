package me.supernb.activity.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import java.util.Locale;
import me.supernb.activity.adapter.rest.response.DrawResponse;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.DrawResult;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/// 活动中心 REST 入口,路径 `/activity/v1/*`。`leaderboard`/`recharges`/`pool`/`recent-draws`
/// 四个只读端点公开免登录;`status`/`draw`/`my-draws`/`usage-leaderboard` 四个端点需要登录——`@CurrentUser` 由 sub2api
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

    /// 构造:注入 CommandBus 与八个查询用例(抽奖状态、充值榜、充值流水、奖池、近期中奖、我的中奖记录、拉新榜、用量榜)。
    public ActivityController(
            CommandBus commandBus,
            DrawStatusQueryService drawStatusQuery,
            LeaderboardQueryService leaderboardQuery,
            RecentRechargesQueryService recentRechargesQuery,
            PoolQueryService poolQuery,
            RecentDrawsQueryService recentDrawsQuery,
            MyDrawsQueryService myDrawsQuery,
            ReferralLeaderboardQueryService referralQuery,
            UsageLeaderboardQueryService usageLeaderboardQuery) {
        this.commandBus = commandBus;
        this.drawStatusQuery = drawStatusQuery;
        this.leaderboardQuery = leaderboardQuery;
        this.recentRechargesQuery = recentRechargesQuery;
        this.poolQuery = poolQuery;
        this.recentDrawsQuery = recentDrawsQuery;
        this.myDrawsQuery = myDrawsQuery;
        this.referralQuery = referralQuery;
        this.usageLeaderboardQuery = usageLeaderboardQuery;
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
}
