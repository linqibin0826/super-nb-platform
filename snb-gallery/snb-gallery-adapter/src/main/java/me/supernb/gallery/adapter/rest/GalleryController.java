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
import me.supernb.gallery.app.usecase.generation.query.GenerationQueries;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptFavoriteCommand;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptLikeCommand;
import me.supernb.gallery.app.usecase.interaction.dto.FavResult;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;
import me.supernb.gallery.app.usecase.interaction.query.InteractionQueries;
import me.supernb.gallery.app.usecase.prompt.query.PromptQueries;
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

/// 灵感库 REST 入口。prompts / categories 公开;互动与生成历史需登录——
/// @CurrentUser 由 sub2api starter 的解析器完成 introspect 校验(active 终端用户,否则 401)。
/// 写操作经 CommandBus 派发,读操作直接注入查询用例。
@RestController
@RequestMapping("/gallery/v1")
public class GalleryController {

    private final CommandBus commandBus;
    private final PromptQueries promptQueries;
    private final InteractionQueries interactionQueries;
    private final GenerationQueries generationQueries;

    public GalleryController(
            CommandBus commandBus,
            PromptQueries promptQueries,
            InteractionQueries interactionQueries,
            GenerationQueries generationQueries) {
        this.commandBus = commandBus;
        this.promptQueries = promptQueries;
        this.interactionQueries = interactionQueries;
        this.generationQueries = generationQueries;
    }

    // —— 公开只读 ——

    @GetMapping("/prompts")
    public Page<PromptSummary> listPrompts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize) {
        return promptQueries.list(category, q, sort, Math.max(1, page), clampSize(pageSize));
    }

    @GetMapping("/prompts/{id}")
    public PromptDetail getPrompt(@PathVariable long id) {
        return promptQueries.detail(id);
    }

    @GetMapping("/categories")
    public CategoryTree categories() {
        return promptQueries.categories();
    }

    // —— 互动(需登录,写经 CommandBus)——

    @PostMapping("/prompts/{id}/like")
    public LikeResult like(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptLikeCommand(id, user.id(), true));
    }

    @DeleteMapping("/prompts/{id}/like")
    public LikeResult unlike(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptLikeCommand(id, user.id(), false));
    }

    @PostMapping("/prompts/{id}/favorite")
    public FavResult favorite(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptFavoriteCommand(id, user.id(), true));
    }

    @DeleteMapping("/prompts/{id}/favorite")
    public FavResult unfavorite(@PathVariable long id, @CurrentUser UserProfile user) {
        return commandBus.handle(new TogglePromptFavoriteCommand(id, user.id(), false));
    }

    @GetMapping("/me/interactions")
    public MyInteractions myInteractions(@RequestParam String ids, @CurrentUser UserProfile user) {
        return interactionQueries.myInteractions(parseIds(ids), user.id());
    }

    @GetMapping("/me/favorites")
    public Page<PromptSummary> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @CurrentUser UserProfile user) {
        return interactionQueries.myFavorites(user.id(), Math.max(1, page), clampSize(pageSize));
    }

    // —— 生成历史(需登录,写经 CommandBus)——

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
                body.id(), user.id(), body.prompt(), body.size(), body.n(), body.quality(), body.status(),
                body.cost(), body.elapsedMs(), body.groupName(), body.keyId(), body.error(), outputs, refs));
    }

    @GetMapping("/me/generations")
    public Page<GenerationSummary> listGenerations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @CurrentUser UserProfile user) {
        return generationQueries.list(user.id(), Math.max(1, page), clampSize(pageSize));
    }

    @GetMapping("/me/generations/{generationId}")
    public GenerationDetail getGeneration(
            @PathVariable String generationId, @CurrentUser UserProfile user) {
        return generationQueries.detail(generationId, user.id());
    }

    @DeleteMapping("/me/generations/{generationId}")
    public DeleteResponse deleteGeneration(
            @PathVariable String generationId, @CurrentUser UserProfile user) {
        commandBus.handle(new DeleteGenerationCommand(generationId, user.id()));
        return new DeleteResponse(true);
    }

    private static int clampSize(int pageSize) {
        return Math.min(48, Math.max(1, pageSize));
    }

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
