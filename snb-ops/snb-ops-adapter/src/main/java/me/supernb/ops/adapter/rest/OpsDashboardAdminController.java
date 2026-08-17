package me.supernb.ops.adapter.rest;

import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.ops.adapter.rest.response.Responses.DashboardResponse;
import me.supernb.ops.app.usecase.query.OpsDashboardQueryService;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 看板端点:待办聚合(纯查询,不推送)。「今天」按站长时区北京时间取。
@RestController
@RequestMapping("/ops/v1/admin")
public class OpsDashboardAdminController {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final OpsDashboardQueryService dashboardQueries;

    /// 构造:注入看板查询服务。
    public OpsDashboardAdminController(OpsDashboardQueryService dashboardQueries) {
        this.dashboardQueries = dashboardQueries;
    }

    /// 待办聚合:30 天内扣款/退款该催了/封号未结案。
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@CurrentUser UserProfile user) {
        OpsAdminGuard.requireAdmin(user);
        return DashboardResponse.of(dashboardQueries.overview(LocalDate.now(BEIJING)));
    }
}
