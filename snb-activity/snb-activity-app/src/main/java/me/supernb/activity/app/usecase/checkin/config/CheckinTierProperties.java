package me.supernb.activity.app.usecase.checkin.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 补给资格三档(spec §5.2):A ¥30~49.99 / B ¥50~499.99 / C ¥500 以上,判定窗口是
/// 当月新增真实充值,顶格档不设上限。展示文案(label/conditionText)是固定运营文案,
/// 不接 env(spec §7.5 env 清单只列阈值/组ID)。⚠️ 本类 Task 14 会追加 group-id/单档成本/
/// 预算相关字段(构造参数列表随之扩展),本任务先建阈值 + 展示文案这一读侧最小可用形态。
@Component
public class CheckinTierProperties {

    /// 单档静态信息(供 GET /checkin/status 的 supply.tiers[] 与 GET /checkin/rewards 的 label 复用)。
    public record TierInfo(String tier, String label, String conditionText, BigDecimal threshold) {
    }

    private final List<TierInfo> tiers;

    /// 构造:三档阈值经配置注入,label/conditionText 固定文案(与 spec §5.2 表格一致)。
    public CheckinTierProperties(
            @Value("${activity.checkin.tier.a.threshold:30}") BigDecimal thresholdA,
            @Value("${activity.checkin.tier.b.threshold:50}") BigDecimal thresholdB,
            @Value("${activity.checkin.tier.c.threshold:500}") BigDecimal thresholdC) {
        this.tiers = List.of(
                new TierInfo("A", "GPT-Plus 补给 · 3 天",
                        "满勤 + 当月新增充值 ¥" + thresholdA.toPlainString() + " 起 · 日限 $5", thresholdA),
                new TierInfo("B", "GPT-Pro 补给 · 3 天",
                        "满勤 + 当月新增充值 ¥" + thresholdB.toPlainString() + " 起 · 日限 $5", thresholdB),
                new TierInfo("C", "GPT-Pro 补给 · 7 天",
                        "满勤 + 当月新增充值 ¥" + thresholdC.toPlainString() + " 起 · 日限 $5", thresholdC));
    }

    /// 三档静态信息,按 A/B/C 顺序(阈值升序)。
    public List<TierInfo> tiers() {
        return tiers;
    }

    /// 按当月新增充值额判定命中档位(顺序遍历,后者覆盖前者等价于取最高达标档);
    /// 未达 A 档门槛返回 empty。
    public Optional<String> tierFor(BigDecimal monthlyRecharge) {
        if (monthlyRecharge == null) {
            return Optional.empty();
        }
        String hit = null;
        for (TierInfo t : tiers) {
            if (monthlyRecharge.compareTo(t.threshold()) >= 0) {
                hit = t.tier();
            }
        }
        return Optional.ofNullable(hit);
    }

    /// 按 tier 代码("A"/"B"/"C")取展示标签;查无返回 tier 原样(防御性兜底,不应触发)。
    public String labelFor(String tier) {
        return tiers.stream().filter(t -> t.tier().equals(tier)).findFirst()
                .map(TierInfo::label).orElse(tier);
    }
}
