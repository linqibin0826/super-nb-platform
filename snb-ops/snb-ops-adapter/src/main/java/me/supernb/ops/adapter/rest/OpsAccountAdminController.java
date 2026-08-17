package me.supernb.ops.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import me.supernb.ops.adapter.rest.request.AccountInput;
import me.supernb.ops.adapter.rest.response.Responses.AccountResponse;
import me.supernb.ops.adapter.rest.response.Responses.IdResponse;
import me.supernb.ops.adapter.rest.response.Responses.SecretResponse;
import me.supernb.ops.app.usecase.account.command.CreateOpsAccountCommand;
import me.supernb.ops.app.usecase.account.command.DeleteOpsAccountCommand;
import me.supernb.ops.app.usecase.account.command.UpdateOpsAccountCommand;
import me.supernb.ops.app.usecase.query.OpsAccountQueryService;
import me.supernb.ops.app.usecase.query.OpsSecretQueryService;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 账号管理端点:列表/增改删/按需解密。全部 role=admin(每方法首行 requireAdmin)。
@RestController
@RequestMapping("/ops/v1/admin")
public class OpsAccountAdminController {

    private final CommandBus commandBus;
    private final OpsAccountQueryService accountQueries;
    private final OpsSecretQueryService secretQueries;

    /// 构造:读注入查询服务,写只注入 CommandBus。
    public OpsAccountAdminController(CommandBus commandBus, OpsAccountQueryService accountQueries,
            OpsSecretQueryService secretQueries) {
        this.commandBus = commandBus;
        this.accountQueries = accountQueries;
        this.secretQueries = secretQueries;
    }

    /// 账号列表(无密文)。
    @GetMapping("/accounts")
    public List<AccountResponse> list(@CurrentUser UserProfile user) {
        OpsAdminGuard.requireAdmin(user);
        return accountQueries.listAll().stream().map(AccountResponse::of).toList();
    }

    /// 建账号。
    @PostMapping("/accounts")
    public IdResponse create(@CurrentUser UserProfile user, @RequestBody AccountInput body) {
        OpsAdminGuard.requireAdmin(user);
        return new IdResponse(commandBus.handle(new CreateOpsAccountCommand(body.email(), body.provider(),
                body.password(), body.recoveryEmail(), body.recoveryPassword(), body.regYear(), body.country(),
                body.owner(), body.statusOrDefault(), body.source(), body.notes())));
    }

    /// 改账号(password null=不改)。
    @PutMapping("/accounts/{id}")
    public void update(@CurrentUser UserProfile user, @PathVariable String id, @RequestBody AccountInput body) {
        OpsAdminGuard.requireAdmin(user);
        commandBus.handle(new UpdateOpsAccountCommand(OpsAdminGuard.idAsLong(id), body.email(), body.provider(),
                body.password(), body.recoveryEmail(), body.recoveryPassword(), body.regYear(), body.country(),
                body.owner(), body.statusOrDefault(), body.source(), body.notes()));
    }

    /// 删账号(名下有订阅 409)。
    @DeleteMapping("/accounts/{id}")
    public void delete(@CurrentUser UserProfile user, @PathVariable String id) {
        OpsAdminGuard.requireAdmin(user);
        commandBus.handle(new DeleteOpsAccountCommand(OpsAdminGuard.idAsLong(id)));
    }

    /// 显示密码(按需解密,列表永远不带)。
    @GetMapping("/accounts/{id}/secret")
    public SecretResponse secret(@CurrentUser UserProfile user, @PathVariable String id) {
        OpsAdminGuard.requireAdmin(user);
        return SecretResponse.of(secretQueries.reveal(OpsAdminGuard.idAsLong(id)));
    }
}
