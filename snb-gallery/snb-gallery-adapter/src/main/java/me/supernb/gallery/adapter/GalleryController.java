package me.supernb.gallery.adapter;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.app.Generations;
import me.supernb.gallery.app.Interactions;
import me.supernb.gallery.app.PromptQueries;
import me.supernb.common.UnauthorizedException;
import me.supernb.sub2api.Sub2apiIntrospectClient;
import me.supernb.sub2api.UserProfile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 灵感库 REST 入口。prompts / categories 公开;互动与生成历史需登录(introspect)。
@RestController
@RequestMapping("/gallery/v1")
public class GalleryController {

    private final PromptQueries promptQueries;
    private final Interactions interactions;
    private final Generations generations;
    private final Sub2apiIntrospectClient introspect;

    public GalleryController(
            PromptQueries promptQueries, Interactions interactions, Generations generations,
            Sub2apiIntrospectClient introspect) {
        this.promptQueries = promptQueries;
        this.interactions = interactions;
        this.generations = generations;
        this.introspect = introspect;
    }

    // —— 公开只读 ——

    @GetMapping("/prompts")
    public GalleryDto.Page<GalleryDto.PromptSummary> listPrompts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize) {
        return promptQueries.list(category, q, sort, Math.max(1, page), clampSize(pageSize));
    }

    @GetMapping("/prompts/{id}")
    public GalleryDto.PromptDetail getPrompt(@PathVariable long id) {
        return promptQueries.detail(id);
    }

    @GetMapping("/categories")
    public GalleryDto.CategoryTree categories() {
        return promptQueries.categories();
    }

    // —— 互动(需登录)——

    @PostMapping("/prompts/{id}/like")
    public Interactions.LikeResult like(@PathVariable long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.like(id, requireUserId(auth), true);
    }

    @DeleteMapping("/prompts/{id}/like")
    public Interactions.LikeResult unlike(@PathVariable long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.like(id, requireUserId(auth), false);
    }

    @PostMapping("/prompts/{id}/favorite")
    public Interactions.FavResult favorite(@PathVariable long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.favorite(id, requireUserId(auth), true);
    }

    @DeleteMapping("/prompts/{id}/favorite")
    public Interactions.FavResult unfavorite(@PathVariable long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.favorite(id, requireUserId(auth), false);
    }

    @GetMapping("/me/interactions")
    public GalleryDto.MyInteractions myInteractions(
            @RequestParam String ids, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.myInteractions(parseIds(ids), requireUserId(auth));
    }

    @GetMapping("/me/favorites")
    public GalleryDto.Page<GalleryDto.PromptSummary> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return interactions.myFavorites(requireUserId(auth), Math.max(1, page), clampSize(pageSize));
    }

    // —— 生成历史(需登录)——

    public record ImagePayload(String b64) {
    }

    public record RefPayload(String b64, String contentType) {
    }

    public record CreateGenerationRequest(
            String id, String prompt, String size, int n, String quality, String status,
            Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<ImagePayload> outputImages, List<RefPayload> refImages) {
    }

    @PostMapping("/me/generations")
    public Generations.Created createGeneration(
            @RequestBody CreateGenerationRequest body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        long userId = requireUserId(auth);
        List<GalleryDto.ImageBytes> outputs = new ArrayList<>();
        if (body.outputImages() != null) {
            for (ImagePayload img : body.outputImages()) {
                outputs.add(new GalleryDto.ImageBytes(Base64.getDecoder().decode(img.b64())));
            }
        }
        List<GalleryDto.RefBytes> refs = new ArrayList<>();
        if (body.refImages() != null) {
            for (RefPayload ref : body.refImages()) {
                refs.add(new GalleryDto.RefBytes(Base64.getDecoder().decode(ref.b64()), ref.contentType()));
            }
        }
        return generations.create(new GalleryDto.CreateGenerationCommand(
                body.id(), userId, body.prompt(), body.size(), body.n(), body.quality(), body.status(),
                body.cost(), body.elapsedMs(), body.groupName(), body.keyId(), body.error(), outputs, refs));
    }

    @GetMapping("/me/generations")
    public GalleryDto.Page<GalleryDto.GenerationSummary> listGenerations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return generations.list(requireUserId(auth), Math.max(1, page), clampSize(pageSize));
    }

    @GetMapping("/me/generations/{generationId}")
    public GalleryDto.GenerationDetail getGeneration(
            @PathVariable String generationId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return generations.detail(generationId, requireUserId(auth));
    }

    @DeleteMapping("/me/generations/{generationId}")
    public DeleteResponse deleteGeneration(
            @PathVariable String generationId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        generations.delete(generationId, requireUserId(auth));
        return new DeleteResponse(true);
    }

    public record DeleteResponse(boolean ok) {
    }

    private long requireUserId(String authorizationHeader) {
        return introspect.introspect(authorizationHeader)
                .filter(UserProfile::isActiveUser)
                .map(UserProfile::id)
                .orElseThrow(UnauthorizedException::new);
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
