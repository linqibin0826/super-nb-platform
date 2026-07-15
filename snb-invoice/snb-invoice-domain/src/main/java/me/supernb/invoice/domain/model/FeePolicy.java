package me.supernb.invoice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// 开票业务常量与手续费策略(2026-07-15 站长拍板,写死不做配置;要调参时再抬成属性类)。
public final class FeePolicy {

    /// 最低开票合计(勾选订单之和须 ≥ 此值)。
    public static final BigDecimal MIN_TOTAL = new BigDecimal("1000");
    /// 免手续费门槛(开票金额 ≥ 此值免收)。
    public static final BigDecimal FREE_THRESHOLD = new BigDecimal("3000");
    /// 手续费率。
    public static final BigDecimal FEE_RATE = new BigDecimal("0.05");
    /// 每用户抬头上限。
    public static final int MAX_PROFILES = 10;
    /// 发票 PDF 大小上限(字节)。
    public static final int MAX_PDF_BYTES = 10 * 1024 * 1024;

    private FeePolicy() {
    }

    /// 勾选合计是否达到最低开票线。
    public static boolean meetsMinimum(BigDecimal amount) {
        return amount.compareTo(MIN_TOTAL) >= 0;
    }

    /// 手续费:≥3000 免,否则 5% HALF_UP 保留两位。申请时刻算好快照进申请单,此后不随策略变。
    public static BigDecimal feeFor(BigDecimal amount) {
        if (amount.compareTo(FREE_THRESHOLD) >= 0) {
            return new BigDecimal("0.00");
        }
        return amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
