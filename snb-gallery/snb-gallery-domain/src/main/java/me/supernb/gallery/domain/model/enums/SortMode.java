package me.supernb.gallery.domain.model.enums;

/// 提示词列表排序模式:FEATURED=收录序(id 倒序,即收录越新越靠前);NEWEST=源发布时间倒序;
/// LIKES/FAVORITES=对应热度(点赞数/收藏数)倒序。
public enum SortMode {
    FEATURED,
    NEWEST,
    LIKES,
    FAVORITES;

    /// 请求字符串 → 排序模式;null 或未识别的值一律回退 FEATURED。
    public static SortMode from(String value) {
        if (value == null) {
            return FEATURED;
        }
        return switch (value) {
            case "newest" -> NEWEST;
            case "likes" -> LIKES;
            case "favorites" -> FAVORITES;
            default -> FEATURED;
        };
    }
}
