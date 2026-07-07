package me.supernb.gallery.app;

import java.time.Instant;
import java.util.List;

/// 灵感库应用层 DTO 汇总(读侧 + 写侧命令)。
public final class GalleryDto {

    private GalleryDto() {
    }

    /// 统一分页信封,对外 JSON {items,total,page,pages}。
    public record Page<T>(List<T> items, long total, int page, int pages) {
        public static <T> Page<T> of(List<T> items, long total, int page, int pageSize) {
            int pages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
            return new Page<>(items, total, page, pages);
        }
    }

    /// 列表瘦身条目(不含 prompt 全文)。
    public record PromptSummary(
            long id, String title, String imageUrl, Integer imageW, Integer imageH,
            String authorName, int likeCount, int favCount) {
    }

    /// 类目(详情内嵌)。
    public record Category(String slug, String axis, String nameZh, String nameEn) {
    }

    /// 单条详情(全字段 + 类目)。
    public record PromptDetail(
            long id, String source, String title, String description, String promptText,
            String lang, String authorName, String authorLink, String sourceLink,
            String imageUrl, Integer imageW, Integer imageH,
            int likeCount, int favCount, Instant sourcePublishedAt, Instant createdAt,
            Category category) {
    }

    /// 类目树节点(带该类目已发布计数)。
    public record CategoryNode(String slug, String nameZh, String nameEn, int count) {
    }

    /// 三轴类目树。
    public record CategoryTree(List<CategoryNode> scene, List<CategoryNode> style, List<CategoryNode> subject) {
    }

    /// 批量互动态(这批 id 里我赞了/藏了哪些)。
    public record MyInteractions(List<Long> liked, List<Long> favorited) {
    }

    /// 生成历史列表条目(thumbUrl 已现签)。
    public record GenerationSummary(
            String id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String error, String thumbUrl) {
    }

    /// 一张图(url 已现签)。
    public record Image(String url, Integer width, Integer height) {
    }

    /// 生成历史详情(全字段 + 输出图 + 参考图)。
    public record GenerationDetail(
            String id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<Image> outputImages, List<Image> refImages) {
    }

    // —— 写侧命令(base64 已由 adapter 解码为字节)——

    public record ImageBytes(byte[] data) {
    }

    public record RefBytes(byte[] data, String contentType) {
    }

    public record CreateGenerationCommand(
            String id, long userId, String prompt, String size, int n, String quality, String status,
            Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<ImageBytes> outputImages, List<RefBytes> refImages) {
    }
}
