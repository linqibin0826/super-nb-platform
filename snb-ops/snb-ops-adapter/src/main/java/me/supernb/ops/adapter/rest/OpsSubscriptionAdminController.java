package me.supernb.ops.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import me.supernb.ops.adapter.rest.request.SubscriptionInput;
import me.supernb.ops.adapter.rest.response.Responses.IdResponse;
import me.supernb.ops.adapter.rest.response.Responses.SubscriptionResponse;
import me.supernb.ops.app.usecase.query.OpsSubscriptionQueryService;
import me.supernb.ops.app.usecase.subscription.command.CreateOpsSubscriptionCommand;
import me.supernb.ops.app.usecase.subscription.command.DeleteOpsSubscriptionCommand;
import me.supernb.ops.app.usecase.subscription.command.UpdateOpsSubscriptionCommand;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 订阅管理端点:列表(全量/按账号)/增改删。全部 role=admin。
@RestController
@RequestMapping("/ops/v1/admin")
public class OpsSubscriptionAdminController {

    private final CommandBus commandBus;
    private final OpsSubscriptionQueryService subscriptionQueries;

    /// 构造:读注入查询服务,写只注入 CommandBus。
    public OpsSubscriptionAdminController(CommandBus commandBus, OpsSubscriptionQueryService subscriptionQueries) {
        this.commandBus = commandBus;
        this.subscriptionQueries = subscriptionQueries;
    }

    /// 订阅列表;accountId 省略=全量。
    @GetMapping("/subscriptions")
    public List<SubscriptionResponse> list(@CurrentUser UserProfile user,
            @RequestParam(required = false) String accountId) {
        OpsAdminGuard.requireAdmin(user);
        var views = accountId == null || accountId.isBlank()
                ? subscriptionQueries.listAll()
                : subscriptionQueries.listByAccount(OpsAdminGuard.idAsLong(accountId));
        return views.stream().map(SubscriptionResponse::of).toList();
    }

    /// 建订阅。
    @PostMapping("/subscriptions")
    public IdResponse create(@CurrentUser UserProfile user, @RequestBody SubscriptionInput body) {
        OpsAdminGuard.requireAdmin(user);
        return new IdResponse(commandBus.handle(new CreateOpsSubscriptionCommand(body.toData())));
    }

    /// 改订阅(全量覆盖)。
    @PutMapping("/subscriptions/{id}")
    public void update(@CurrentUser UserProfile user, @PathVariable String id,
            @RequestBody SubscriptionInput body) {
        OpsAdminGuard.requireAdmin(user);
        commandBus.handle(new UpdateOpsSubscriptionCommand(OpsAdminGuard.idAsLong(id), body.toData()));
    }

    /// 删订阅。
    @DeleteMapping("/subscriptions/{id}")
    public void delete(@CurrentUser UserProfile user, @PathVariable String id) {
        OpsAdminGuard.requireAdmin(user);
        commandBus.handle(new DeleteOpsSubscriptionCommand(OpsAdminGuard.idAsLong(id)));
    }
}
