package me.supernb.invoice.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceDetailDto;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceItem;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.model.read.InvoiceRequestView;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.model.read.ProfileView;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;

/// /invoice/v1 响应形状(全部 record;实体 id 一律字符串)。
public final class Responses {

    private Responses() {
    }

    /// 通用 {"id": "..."}。
    public record IdResponse(String id) {
    }

    /// 通用 {"status": "..."}。
    public record StatusResponse(String status) {
    }

    /// 抬头行(verifiedAt=核验章,null 即未核验)。
    public record ProfileResponse(String id, String type, String title, String taxNo, String regAddress,
                                  String regPhone, String bankName, String bankAccount, Instant verifiedAt) {
        public static ProfileResponse of(ProfileView v) {
            return new ProfileResponse(String.valueOf(v.id()), v.type().name(), v.title(), v.taxNo(),
                    v.regAddress(), v.regPhone(), v.bankName(), v.bankAccount(), v.verifiedAt());
        }
    }

    /// 抬头核验结果:found=false=供应商查无此企业(≠出错,出错走异常 422/429)。
    public record RegistryLookupResponse(boolean found, RegistryCompany official) {
        /// 官方开票资料(字段已清洗:strip、空归 null、地址电话已拆)。
        public record RegistryCompany(String name, String taxNo, String address, String phone,
                                      String bankName, String bankAccount) {
        }

        public static RegistryLookupResponse of(Optional<CompanyRecord> record) {
            return record.map(r -> new RegistryLookupResponse(true, new RegistryCompany(
                            r.name(), r.taxNo(), r.address(), r.phone(), r.bankName(), r.bankAccount())))
                    .orElseGet(() -> new RegistryLookupResponse(false, null));
        }
    }

    /// 可开票订单行。
    public record OrderResponse(String orderId, String orderNo, BigDecimal amount, Instant completedAt) {
        public static OrderResponse of(OrderLine l) {
            return new OrderResponse(String.valueOf(l.orderId()), l.orderNo(), l.amount(), l.completedAt());
        }
    }

    /// 可开票总览(常量随行下发,前端预估手续费不用硬编码)。
    public record OrdersOverviewResponse(List<OrderResponse> orders, BigDecimal billableTotal, BigDecimal balance,
                                         BigDecimal minTotal, BigDecimal freeThreshold, BigDecimal feeRate) {
        public static OrdersOverviewResponse of(List<OrderLine> orders, BigDecimal total, BigDecimal balance) {
            return new OrdersOverviewResponse(orders.stream().map(OrderResponse::of).toList(), total, balance,
                    FeePolicy.MIN_TOTAL, FeePolicy.FREE_THRESHOLD, FeePolicy.FEE_RATE);
        }
    }

    /// 我的申请行。
    public record RequestResponse(String id, String requestNo, BigDecimal amount, BigDecimal fee, String status,
                                  String profileTitle, String remark, String rejectReason,
                                  Instant createdAt, Instant issuedAt) {
        public static RequestResponse of(InvoiceRequestView v) {
            return new RequestResponse(String.valueOf(v.id()), v.requestNo(), v.amount(), v.fee(),
                    v.status().name(), v.profileTitle(), v.remark(), v.rejectReason(), v.createdAt(), v.issuedAt());
        }
    }

    /// 管理端列表行(+完整邮箱)。
    public record AdminRowResponse(String id, String requestNo, String userId, String email, BigDecimal amount,
                                   BigDecimal fee, String status, Instant createdAt) {
        public static AdminRowResponse of(AdminInvoiceItem item) {
            var r = item.row();
            return new AdminRowResponse(String.valueOf(r.id()), r.requestNo(), String.valueOf(r.userId()),
                    item.email(), r.amount(), r.fee(), r.status().name(), r.createdAt());
        }
    }

    /// 管理端分页信封。
    public record AdminPageResponse(List<AdminRowResponse> items, long total) {
    }

    /// 管理端详情(抬头快照平铺——含提交时核验章——+订单明细)。
    public record AdminDetailResponse(String id, String requestNo, String userId, String email, BigDecimal amount,
                                      BigDecimal fee, String status, String profileType, String profileTitle,
                                      String profileTaxNo, String profileRegAddress, String profileRegPhone,
                                      String profileBankName, String profileBankAccount, Instant profileVerifiedAt,
                                      String remark, String rejectReason, Instant feeChargedAt, Instant issuedAt,
                                      Instant createdAt, List<OrderResponse> orders) {
        public static AdminDetailResponse of(AdminInvoiceDetailDto dto) {
            var d = dto.detail();
            return new AdminDetailResponse(String.valueOf(d.id()), d.requestNo(), String.valueOf(d.userId()),
                    dto.email(), d.amount(), d.fee(), d.status().name(), d.profileType().name(), d.profileTitle(),
                    d.profileTaxNo(), d.profileRegAddress(), d.profileRegPhone(), d.profileBankName(),
                    d.profileBankAccount(), d.profileVerifiedAt(), d.remark(), d.rejectReason(), d.feeChargedAt(),
                    d.issuedAt(), d.createdAt(), d.orders().stream().map(OrderResponse::of).toList());
        }
    }
}
