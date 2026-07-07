package me.supernb.gallery.domain.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// 生成历史聚合仓储端口(4 表一事务)。
///
/// 身份即雪花 id(验收意见⑦,单一身份):经 [#nextId()] 预分配——
/// R2 对象键(`gen/{userId}/{id}/…`)需要在持久化之前确定,故由用例先取号再落库。
public interface GenerationRepository {

    /// 落库载荷:聚合根字段 + 输出图 + 参考图(id 为预分配雪花)。
    record SaveGeneration(
            long id, long userId, String prompt, String size, int n, String quality, String status,
            Double cost, int elapsedMs, String groupName, Long keyId, String error, String thumbKey,
            List<OutputImage> outputs, List<RefImage> refs) {
    }

    /// 一张输出图(键已上传)。
    record OutputImage(int idx, String r2Key, int bytes) {
    }

    /// 一张参考图(内容寻址)。
    record RefImage(int idx, String sha256, String r2Key, int bytes) {
    }

    /// 列表行(读投影,id 为雪花)。
    record ListRow(
            long id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String error, String thumbKey) {
    }

    /// 一页列表行 + 总数。
    record PageRows(List<ListRow> rows, long total) {
    }

    /// 详情行(读投影,含输出图键与参考图键)。
    record DetailRow(
            long id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<String> outputKeys, List<String> refKeys) {
    }

    /// 预分配下一个生成记录 id(雪花)。
    long nextId();

    /// 用户参考图库中是否已有该内容哈希。
    boolean refExists(long userId, String sha256);

    /// 落库(4 表一事务),返回创建时刻。
    Instant save(SaveGeneration cmd);

    /// 用户生成历史分页(创建时刻倒序)。
    PageRows list(long userId, int page, int pageSize);

    /// 单条详情;不存在或不归属该用户返回 empty。
    Optional<DetailRow> detail(long id, long userId);

    /// 删除并返回全部 R2 对象键(含缩略图);不存在返回 empty。
    Optional<List<String>> deleteReturningObjectKeys(long id, long userId);
}
