package me.supernb.gallery.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import me.supernb.gallery.adapter.rest.request.CreateGenerationRequest;
import me.supernb.gallery.adapter.rest.response.DeleteResponse;
import me.supernb.gallery.app.usecase.generation.command.CreateGenerationCommand;
import me.supernb.gallery.app.usecase.generation.command.DeleteGenerationCommand;
import me.supernb.gallery.app.usecase.generation.command.ImageBytes;
import me.supernb.gallery.app.usecase.generation.command.RefBytes;
import me.supernb.gallery.app.usecase.generation.dto.Created;
import me.supernb.gallery.app.usecase.generation.query.GenerationQueryService;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptFavoriteCommand;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptLikeCommand;
import me.supernb.gallery.app.usecase.interaction.dto.FavResult;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;
import me.supernb.gallery.app.usecase.interaction.query.InteractionQueryService;
import me.supernb.gallery.app.usecase.prompt.query.PromptQueryService;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.GenerationDetail;
import me.supernb.gallery.domain.model.read.GenerationSummary;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 灵感库 REST 入口,路径 `/gallery/v1/*`。`prompts`/`categories` 只读端点公开免登录;
/// 互动(点赞/收藏)与生成历史端点需要登录——`@CurrentUser` 由 sub2api starter 的解析器
/// 完成 introspect 校验(要求 active 的 user 或 admin 账号,否则 401)。写操作组装命令
/// 经 `CommandBus` 派发,只读端点直接调用注入的查询用例。
@RestController
@RequestMapping("/gallery/v1")
public class GalleryController {

    private final CommandBus commandBus;
    private final PromptQueryService promptQueryService;
    private final InteractionQueryService interactionQueryService;
    private final GenerationQueryService generationQueryService;

    /// 构造:注入 CommandBus 与三个查询用例(提示词、互动、生成历史)。
    public GalleryController(
            CommandBus commandBus,
            PromptQueryService promptQueryService,
            InteractionQueryService interactionQueryService,
            GenerationQueryService generationQueryService) {
        this.commandBus = commandBus;
        this.promptQueryService = promptQueryService;
        this.interactionQueryService = interactionQueryService;
        this.generationQueryService = generationQueryService;
    }

    // —— 公开只读 ——

    /// 提示词列表(公开),按类目/关键字/排序分页。
    @GetMapping("/prompts")
    public Page<PromptSummary> listPrompts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize) {
        return promptQueryService.list(category, q, sort, Math.max(1, page), clampSize(pageSize));
    }

    /// 提示词详情(公开)。
    @GetMapping("/prompts/{id}")
    public PromptDetail getPrompt(@PathVariable long id) {
        return promptQueryService.detail(id);
    }

    /// 三轴类目树(公开)。
    @GetMapping("/categories")
    public CategoryTree categories() {
        return promptQueryService.categories();
    }

    // —— 互动(需登录,写经 CommandBus)——

    /// 点赞(需登录)。
    @PostMapping("/prompts/{id}/like")
    public LikeResult like(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptLikeCommand(id, user.id(), true));
    }

    /// 取消点赞(需登录)。
    @DeleteMapping("/prompts/{id}/like")
    public LikeResult unlike(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptLikeCommand(id, user.id(), false));
    }

    /// 收藏(需登录)。
    @PostMapping("/prompts/{id}/favorite")
    public FavResult favorite(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptFavoriteCommand(id, user.id(), true));
    }

    /// 取消收藏(需登录)。
    @DeleteMapping("/prompts/{id}/favorite")
    public FavResult unfavorite(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptFavoriteCommand(id, user.id(), false));
    }

    /// 批量互动态回显(需登录)。
    @GetMapping("/me/interactions")
    public MyInteractions myInteractions(@RequestParam String ids, @CurrentUser UserProfile user) {
        return interactionQueryService.myInteractions(parseIds(ids), user.id());
    }

    /// 我的收藏分页(需登录)。
    @GetMapping("/me/favorites")
    public Page<PromptSummary> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @CurrentUser UserProfile user) {
        return interactionQueryService.myFavorites(user.id(), Math.max(1, page), clampSize(pageSize));
    }

    // —— 生成历史(需登录,写经 CommandBus)——

    /// 创建一条生成记录(需登录)。把请求体里 base64 编码的输出图/参考图解码成字节数组,
    /// 缺省(null)按空列表处理,组装成命令派发;没有创建幂等预检,重复提交各自落一行——
    /// 防重复提交靠前端队列的单飞语义兜底,不在服务端做。
    // @RequestBody 在前、@CurrentUser 在后:保持「坏 JSON 先 400、再谈 401」的旧语义(参数按序解析)
    @PostMapping("/me/generations")
    public Created createGeneration(
            @RequestBody CreateGenerationRequest body, @CurrentUser UserProfile user) {
        List<ImageBytes> outputs = new ArrayList<>();
        if (body.outputImages() != null) {
            for (CreateGenerationRequest.ImagePayload img : body.outputImages()) {
                outputs.add(new ImageBytes(Base64.getDecoder().decode(img.b64())));
            }
        }
        List<RefBytes> refs = new ArrayList<>();
        if (body.refImages() != null) {
            for (CreateGenerationRequest.RefPayload ref : body.refImages()) {
                refs.add(new RefBytes(Base64.getDecoder().decode(ref.b64()), ref.contentType()));
            }
        }
        return commandBus.handle(new CreateGenerationCommand(
                user.id(), body.prompt(), body.size(), body.n(), body.quality(), body.status(),
                body.cost(), body.elapsedMs(), body.groupName(), body.keyId(), body.error(), outputs, refs));
    }

    /// 生成历史分页列表(需登录)。
    @GetMapping("/me/generations")
    public Page<GenerationSummary> listGenerations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @CurrentUser UserProfile user) {
        return generationQueryService.list(user.id(), Math.max(1, page), clampSize(pageSize));
    }

    /// 生成记录详情(需登录)。不存在或不归属当前用户,统一 404。
    @GetMapping("/me/generations/{generationId}")
    public GenerationDetail getGeneration(
            @PathVariable long generationId, @CurrentUser UserProfile user) {
        return generationQueryService.detail(generationId, user.id());
    }

    /// 删除本人一条生成记录(需登录),级联清理 R2 对象;不存在或不归属当前用户 → 404。
    @DeleteMapping("/me/generations/{generationId}")
    public DeleteResponse deleteGeneration(
            @PathVariable long generationId, @CurrentUser UserProfile user) {
        commandBus.handle(new DeleteGenerationCommand(generationId, user.id()));
        return new DeleteResponse(true);
    }

    /// 页大小钳制到 [1,48]。
    private static int clampSize(int pageSize) {
        return Math.min(48, Math.max(1, pageSize));
    }

    /// 解析逗号分隔的 id 字符串:非 1~19 位纯数字的片段直接跳过(不抛异常),
    /// 累计满 100 个即停止扫描,不再处理输入里剩余的部分——防御 `ids` 查询参数被喂超长串。
    private static List<Long> parseIds(String ids) {
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            String trimmed = part.strip();
            if (trimmed.matches("\\d{1,19}")) {
                result.add(Long.parseLong(trimmed));
            }
            if (result.size() >= 100) {
                break;
            }
        }
        return result;
    }
}
