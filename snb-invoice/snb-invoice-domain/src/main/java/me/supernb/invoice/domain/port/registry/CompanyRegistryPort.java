package me.supernb.invoice.domain.port.registry;

import java.util.Optional;

/// 企业官方开票资料查询端口(第三方数据源,按次付费)。两个入口的成本语义截然不同:
/// [#lookup] 缓存未命中会发起真实付费调用;[#cached] 只碰本地缓存、永远免费——
/// 保存抬头时的盖章比对必须走 cached,绝不能因为一次保存烧一次配额。
public interface CompanyRegistryPort {

    /// 官方开票资料。实现负责把字段清洗干净:全部 strip、空串归 null;
    /// phone 从供应商的地址电话混排里拆出,拆不出时为 null(电话留在 address 里)。
    record CompanyRecord(String name, String taxNo, String address, String phone,
                         String bankName, String bankAccount) {
    }

    /// 按企业名称查官方开票资料(缓存优先,未命中打真实供应商=付费)。
    /// empty = 供应商明确查无此企业;供应商不可用(未配置/网络/配额)抛 registryUnavailable。
    Optional<CompanyRecord> lookup(String enterpriseName);

    /// 只查本地缓存,绝不发起付费调用。empty = 缓存没有(≠查无此企业)。
    Optional<CompanyRecord> cached(String enterpriseName);
}
