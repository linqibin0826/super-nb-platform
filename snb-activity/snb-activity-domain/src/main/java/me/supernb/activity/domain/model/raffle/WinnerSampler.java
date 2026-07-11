package me.supernb.activity.domain.model.raffle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/// 中奖抽样纯函数:不放回、一人至多一次,返回长度 = min(prizeCount, 候选数),
/// 顺序即奖品分配顺序(第 i 个中奖者拿 sort_order 第 i 件)。
/// EQUAL 均匀抽;WEIGHTED 按候选权重轮盘抽,总权重非正时退化为均匀(防御空池语义)。
/// rng 由调用方注入:生产 SecureRandom,测试固定种子——保证可测的确定性。
public final class WinnerSampler {

    private WinnerSampler() {}

    /// 候选:userId + 权重(WEIGHTED 模式下=门槛指标复核值;EQUAL 模式忽略)。
    public record Candidate(long userId, BigDecimal weight) {}

    /// 执行抽样,见类注释。candidates 不被修改(内部复制)。
    public static List<Long> pick(List<Candidate> candidates, int prizeCount, WeightMode mode,
            RandomGenerator rng) {
        List<Candidate> pool = new ArrayList<>(candidates);
        int n = Math.min(prizeCount, pool.size());
        List<Long> winners = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int idx = mode == WeightMode.WEIGHTED ? weightedIndex(pool, rng) : rng.nextInt(pool.size());
            winners.add(pool.remove(idx).userId());
        }
        return winners;
    }

    /// 轮盘法取一个下标;总权重 <= 0 时退化均匀。
    private static int weightedIndex(List<Candidate> pool, RandomGenerator rng) {
        double total = pool.stream().map(Candidate::weight).mapToDouble(BigDecimal::doubleValue).sum();
        if (total <= 0) {
            return rng.nextInt(pool.size());
        }
        double r = rng.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < pool.size(); i++) {
            acc += pool.get(i).weight().doubleValue();
            if (r < acc) {
                return i;
            }
        }
        return pool.size() - 1; // 浮点边缘兜底
    }
}
