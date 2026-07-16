package me.supernb.guide.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.Set;
import me.supernb.guide.app.usecase.command.AckGuideCommand;
import me.supernb.guide.app.usecase.query.MyGuideAcksQueryService;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 通用引导已读 REST:任何前端站的首次引导都走这套(key 命名空间自declare,如 invoice.intro.v1)。
/// 全端点要求登录(@CurrentUser → introspect),数据按当前用户隔离。
@RestController
@RequestMapping("/guide/v1")
public class GuideController {

    private final CommandBus commandBus;
    private final MyGuideAcksQueryService ackQueries;

    /// 构造:读注入查询服务,写只注入 CommandBus。
    public GuideController(CommandBus commandBus, MyGuideAcksQueryService ackQueries) {
        this.commandBus = commandBus;
        this.ackQueries = ackQueries;
    }

    /// 我的已读 key 集合。
    @GetMapping("/acks")
    public AcksResponse myAcks(@CurrentUser UserProfile user) {
        return new AcksResponse(ackQueries.ackedKeys(user.id()));
    }

    /// 标记某引导已读(幂等,重复调用同样 204)。
    @PostMapping("/acks/{key}")
    public ResponseEntity<Void> ack(@CurrentUser UserProfile user, @PathVariable String key) {
        commandBus.handle(new AckGuideCommand(user.id(), key));
        return ResponseEntity.noContent().build();
    }

    /// 已读集合响应。
    public record AcksResponse(Set<String> keys) {
    }
}
