package me.supernb.activity.adapter;

import java.math.BigDecimal;
import java.util.List;
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.GetDrawStatusUseCase;
import me.supernb.activity.app.GetLeaderboardUseCase;
import me.supernb.activity.app.GetMyDrawsUseCase;
import me.supernb.activity.app.GetPoolUseCase;
import me.supernb.activity.app.GetRecentDrawsUseCase;
import me.supernb.activity.app.GetRecentRechargesUseCase;
import me.supernb.activity.app.PerformDrawUseCase;
import me.supernb.activity.domain.DrawResult;
import me.supernb.common.UnauthorizedException;
import me.supernb.sub2api.Sub2apiIntrospectClient;
import me.supernb.sub2api.UserProfile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 活动中心 REST 入口。公开端点(榜单/流水/奖池/近期中奖)免登录;
/// status / draw / my-draws 需登录(introspect 校验 active 终端用户,否则 401)。
@RestController
@RequestMapping("/activity/v1")
public class ActivityController {

    private final GetDrawStatusUseCase getDrawStatus;
    private final PerformDrawUseCase performDraw;
    private final GetLeaderboardUseCase getLeaderboard;
    private final GetRecentRechargesUseCase getRecentRecharges;
    private final GetPoolUseCase getPool;
    private final GetRecentDrawsUseCase getRecentDraws;
    private final GetMyDrawsUseCase getMyDraws;
    private final Sub2apiIntrospectClient introspect;

    public ActivityController(
            GetDrawStatusUseCase getDrawStatus,
            PerformDrawUseCase performDraw,
            GetLeaderboardUseCase getLeaderboard,
            GetRecentRechargesUseCase getRecentRecharges,
            GetPoolUseCase getPool,
            GetRecentDrawsUseCase getRecentDraws,
            GetMyDrawsUseCase getMyDraws,
            Sub2apiIntrospectClient introspect) {
        this.getDrawStatus = getDrawStatus;
        this.performDraw = performDraw;
        this.getLeaderboard = getLeaderboard;
        this.getRecentRecharges = getRecentRecharges;
        this.getPool = getPool;
        this.getRecentDraws = getRecentDraws;
        this.getMyDraws = getMyDraws;
        this.introspect = introspect;
    }

    /// 抽奖结果响应。
    public record DrawResponse(BigDecimal amount, String redeemCode, boolean consolation) {
    }

    @GetMapping("/leaderboard")
    public List<ActivityDto.LeaderEntry> leaderboard() {
        return getLeaderboard.leaderboard();
    }

    @GetMapping("/recharges")
    public List<ActivityDto.RechargeEntry> recharges() {
        return getRecentRecharges.recentRecharges();
    }

    @GetMapping("/pool")
    public List<ActivityDto.PoolTier> pool() {
        return getPool.pool();
    }

    @GetMapping("/recent-draws")
    public List<ActivityDto.PublicDraw> recentDraws() {
        return getRecentDraws.recentDraws();
    }

    @GetMapping("/status")
    public ActivityDto.DrawStatus status(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return getDrawStatus.status(requireUserId(auth));
    }

    @PostMapping("/draw")
    public DrawResponse draw(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        DrawResult r = performDraw.draw(requireUserId(auth));
        return new DrawResponse(r.amount(), r.redeemCode(), r.consolation());
    }

    @GetMapping("/my-draws")
    public List<ActivityDto.MyDrawView> myDraws(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return getMyDraws.myDraws(requireUserId(auth));
    }

    /// 校验登录态:introspect 转发 sub2api,要求 active 终端用户,否则 401。
    private long requireUserId(String authorizationHeader) {
        return introspect.introspect(authorizationHeader)
                .filter(UserProfile::isActiveUser)
                .map(UserProfile::id)
                .orElseThrow(UnauthorizedException::new);
    }
}
