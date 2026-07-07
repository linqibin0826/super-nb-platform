package me.supernb.gallery.domain.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// 生成历史聚合仓储端口:generation + generation_image + generation_ref + ref_image 四张表,
/// 写路径一个事务落库/删库。
///
/// 身份即雪花 id(验收意见⑦,单一身份),经 [#nextId()] 预分配:R2 对象键
/// (`gen/{userId}/{id}/…`)必须先于持久化确定,故用例先取号、按号命名并上传,再带着确定的 id 落库。
/// `list`/`detail` 两个读方法留在这里没有拆出单独的 ReadPort:读的是这个聚合自己的表,
/// 不是跨聚合投影。
public interface GenerationRepository {

    /// 落库载荷:聚合根字段 + 输出图 + 参考图,id 为 [#nextId()] 预分配的雪花。
    ///
    /// @param id        预分配生成记录 id
    /// @param userId    生成发起用户(sub2api user id)
    /// @param prompt    生成用提示词
    /// @param size      尺寸档,如 `1024x1024`
    /// @param n         出图张数
    /// @param quality   画质档
    /// @param status    任务终态:`done` | `error`
    /// @param cost      本次消耗额度(USD 名义计价),可空
    /// @param elapsedMs 生成耗时毫秒
    /// @param groupName 计费分组名
    /// @param keyId     使用的 API Key id,可空
    /// @param error     失败原因;成功时为 null
    /// @param thumbKey  256px 缩略图 R2 键,可空(缩略图生成失败时为 null,列表侧回退首张输出图)
    /// @param outputs   输出图列表(已上传)
    /// @param refs      参考图列表(内容寻址,可能复用已存在的 ref_image)
    record SaveGeneration(
            long id, long userId, String prompt, String size, int n, String quality, String status,
            Double cost, int elapsedMs, String groupName, Long keyId, String error, String thumbKey,
            List<OutputImage> outputs, List<RefImage> refs) {
    }

    /// 一张输出图,键已上传完成。
    ///
    /// @param idx   输出序号,0 起
    /// @param r2Key R2 对象键
    /// @param bytes 字节数
    record OutputImage(int idx, String r2Key, int bytes) {
    }

    /// 一张参考图,按内容哈希寻址(供跨生成记录去重复用)。
    ///
    /// @param idx    参考序号,0 起
    /// @param sha256 内容哈希(去重身份,联合 userId 定位 ref_image)
    /// @param r2Key  R2 对象键
    /// @param bytes  字节数
    record RefImage(int idx, String sha256, String r2Key, int bytes) {
    }

    /// 生成历史列表行(读投影)。缩略图字段是原始 R2 键,现签成 URL 是调用方的事。
    ///
    /// @param id        生成记录 id(雪花)
    /// @param createdAt 落库(创建)时刻
    /// @param prompt    生成用提示词
    /// @param size      尺寸档
    /// @param n         出图张数
    /// @param quality   画质档
    /// @param status    任务终态:`done` | `error`
    /// @param cost      本次消耗额度(USD 名义计价),可空
    /// @param elapsedMs 生成耗时毫秒
    /// @param error     失败原因;成功时为 null
    /// @param thumbKey  256px 缩略图 R2 键,可空(生成失败且无输出图时无键可给)
    record ListRow(
            long id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String error, String thumbKey) {
    }

    /// 一页列表行 + 总数(端口返回原始行,不是对外的 `Page` 读视图信封;调用方按需要再组装)。
    ///
    /// @param rows  本页生成记录列表行
    /// @param total 总条数
    record PageRows(List<ListRow> rows, long total) {
    }

    /// 生成记录详情行(读投影)。输出图/参考图给的是原始 R2 键,现签成 URL 是调用方的事。
    ///
    /// @param id         生成记录 id(雪花)
    /// @param createdAt  落库(创建)时刻
    /// @param prompt     生成用提示词
    /// @param size       尺寸档
    /// @param n          出图张数
    /// @param quality    画质档
    /// @param status     任务终态:`done` | `error`
    /// @param cost       本次消耗额度(USD 名义计价),可空
    /// @param elapsedMs  生成耗时毫秒
    /// @param groupName  计费分组名
    /// @param keyId      使用的 API Key id,可空
    /// @param error      失败原因;成功时为 null
    /// @param outputKeys 输出图 R2 键列表(按序号排好)
    /// @param refKeys    参考图 R2 键列表(按序号排好)
    record DetailRow(
            long id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<String> outputKeys, List<String> refKeys) {
    }

    /// 预分配下一个生成记录 id(雪花),供用例先取号再按号命名 R2 对象、最后落库。
    long nextId();

    /// 用户参考图库中是否已有该内容哈希;调用方据此决定是否跳过重复上传。
    boolean refExists(long userId, String sha256);

    /// 落库(4 表一个事务),返回落库产生的创建时刻。
    Instant save(SaveGeneration cmd);

    /// 用户生成历史分页,按创建时刻倒序(page 1 起)。
    PageRows list(long userId, int page, int pageSize);

    /// 单条详情;不存在或不归属该用户返回 empty。
    Optional<DetailRow> detail(long id, long userId);

    /// 删除生成记录(4 表一个事务)并返回其全部 R2 对象键(含缩略图);
    /// R2 对象清理由调用方另经 `ImageStoragePort` 完成,这个端口只管数据库事务。
    /// 不存在或不归属该用户返回 empty。
    Optional<List<String>> deleteReturningObjectKeys(long id, long userId);
}
