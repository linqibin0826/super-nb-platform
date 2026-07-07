package me.supernb.activity.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import me.supernb.activity.adapter.rest.response.DrawResponse;
import me.supernb.activity.app.usecase.campaign.query.GetLeaderboardUseCase;
import me.supernb.activity.app.usecase.campaign.query.GetPoolUseCase;
import me.supernb.activity.app.usecase.campaign.query.GetRecentRechargesUseCase;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.query.GetDrawStatusUseCase;
import me.supernb.activity.app.usecase.draw.query.GetMyDrawsUseCase;
import me.supernb.activity.app.usecase.draw.query.GetRecentDrawsUseCase;
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
    private final GetDrawStatusUseCase getDrawStatus;
    private final GetLeaderboardUseCase getLeaderboard;
    private final GetRecentRechargesUseCase getRecentRecharges;
    private final GetPoolUseCase getPool;
    private final GetRecentDrawsUseCase getRecentDraws;
    private final GetMyDrawsUseCase getMyDraws;

    public ActivityController(
            CommandBus commandBus,
            GetDrawStatusUseCase getDrawStatus,
            GetLeaderboardUseCase getLeaderboard,
            GetRecentRechargesUseCase getRecentRecharges,
            GetPoolUseCase getPool,
            GetRecentDrawsUseCase getRecentDraws,
            GetMyDrawsUseCase getMyDraws) {
        this.commandBus = commandBus;
        this.getDrawStatus = getDrawStatus;
        this.getLeaderboard = getLeaderboard;
        this.getRecentRecharges = getRecentRecharges;
        this.getPool = getPool;
        this.getRecentDraws = getRecentDraws;
        this.getMyDraws = getMyDraws;
    }

    @GetMapping("/leaderboard")
    public List<LeaderEntry> leaderboard() {
        return getLeaderboard.leaderboard();
    }

    @GetMapping("/recharges")
    public List<RechargeEntry> recharges() {
        return getRecentRecharges.recentRecharges();
    }

    @GetMapping("/pool")
    public List<PoolTier> pool() {
        return getPool.pool();
    }

    @GetMapping("/recent-draws")
    public List<PublicDraw> recentDraws() {
        return getRecentDraws.recentDraws();
    }

    @GetMapping("/status")
    public DrawStatus status(@CurrentUser UserProfile user) {
        return getDrawStatus.status(user.id());
    }

    @PostMapping("/draw")
    public DrawResponse draw(@CurrentUser UserProfile user) {
        DrawResult r = commandBus.handle(new PerformDrawCommand(user.id()));
        return new DrawResponse(r.amount(), r.redeemCode(), r.consolation());
    }

    @GetMapping("/my-draws")
    public List<MyDrawView> myDraws(@CurrentUser UserProfile user) {
        return getMyDraws.myDraws(user.id());
    }
}
