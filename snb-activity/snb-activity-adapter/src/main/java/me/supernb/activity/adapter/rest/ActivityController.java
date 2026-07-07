package me.supernb.activity.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import me.supernb.activity.adapter.rest.response.DrawResponse;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.model.read.MyDrawView;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.model.read.PublicDraw;
import me.supernb.activity.domain.model.read.RechargeEntry;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 活动中心 REST 入口。公开端点(榜单/流水/奖池/近期中奖)免登录;
/// status / draw / my-draws 需登录——@CurrentUser 由 sub2api starter 的解析器完成
/// introspect 校验(active 终端用户,否则 401)。写操作经 CommandBus 派发,读操作直接注入查询用例。
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

    public ActivityController(
            CommandBus commandBus,
            DrawStatusQueryService drawStatusQuery,
            LeaderboardQueryService leaderboardQuery,
            RecentRechargesQueryService recentRechargesQuery,
            PoolQueryService poolQuery,
            RecentDrawsQueryService recentDrawsQuery,
            MyDrawsQueryService myDrawsQuery) {
        this.commandBus = commandBus;
        this.drawStatusQuery = drawStatusQuery;
        this.leaderboardQuery = leaderboardQuery;
        this.recentRechargesQuery = recentRechargesQuery;
        this.poolQuery = poolQuery;
        this.recentDrawsQuery = recentDrawsQuery;
        this.myDrawsQuery = myDrawsQuery;
    }

    @GetMapping("/leaderboard")
    public List<LeaderEntry> leaderboard() {
        return leaderboardQuery.leaderboard();
    }

    @GetMapping("/recharges")
    public List<RechargeEntry> recharges() {
        return recentRechargesQuery.recentRecharges();
    }

    @GetMapping("/pool")
    public List<PoolTier> pool() {
        return poolQuery.pool();
    }

    @GetMapping("/recent-draws")
    public List<PublicDraw> recentDraws() {
        return recentDrawsQuery.recentDraws();
    }

    /// 我的抽奖资格与剩余次数。
    @GetMapping("/status")
    public DrawStatus status(@CurrentUser UserProfile user) {
        return drawStatusQuery.status(user.id());
    }

    /// 抽一次(经 CommandBus 派发)。
    @PostMapping("/draw")
    public DrawResponse draw(@CurrentUser UserProfile user) {
        DrawResult r = commandBus.handle(new PerformDrawCommand(user.id()));
        return new DrawResponse(r.amount(), r.redeemCode(), r.consolation());
    }

    @GetMapping("/my-draws")
    public List<MyDrawView> myDraws(@CurrentUser UserProfile user) {
        return myDrawsQuery.myDraws(user.id());
    }
}
