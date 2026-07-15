package me.supernb.invoice.domain.port.repository;

import java.util.Optional;
import me.supernb.invoice.domain.model.ProfileType;

/// 抬头聚合持久化端口。全部操作按 (userId, profileId) 双键定位——归属校验即隔离。
public interface InvoiceProfileRepository {

    /// 抬头数据(创建/更新入参与快照读出共用一个形状)。
    record ProfileData(ProfileType type, String title, String taxNo, String regAddress,
                       String regPhone, String bankName, String bankAccount) {
    }

    /// 新建抬头,返回雪花 id。
    long create(long userId, ProfileData data);

    /// 全量覆盖更新;false = 不存在或不属于该用户。
    boolean update(long userId, long profileId, ProfileData data);

    /// 硬删(申请单持有独立快照,删抬头不影响历史);false = 不存在或不属于该用户。
    boolean delete(long userId, long profileId);

    /// 按归属取抬头(申请时做快照用)。
    Optional<ProfileData> find(long userId, long profileId);

    /// 该用户抬头数(上限守卫用)。
    int countByUser(long userId);
}
