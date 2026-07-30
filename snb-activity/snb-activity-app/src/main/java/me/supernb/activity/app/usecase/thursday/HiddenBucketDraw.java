package me.supernb.activity.app.usecase.thursday;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/// 隐藏款(瑞幸)摇号:**确定性**抽签,零存储。
///
/// 种子 = SHA-256(salt | 场次日期 | 开奖时刻的桶数)。同样的输入永远摇出同一组号,
/// 所以不需要建表存结果、不需要定时任务、天然幂等——**也就天然不可能"改判"**
/// (spec §6:规则上架即定死,公示后不改判)。
///
/// 为什么种子里必须有桶数:抽签范围是 1..N。N 在开奖时刻冻结,冻结前不揭晓,
/// 否则先充的人占尽便宜、后面的桶根本没资格中。
///
/// salt 的作用:不配也能跑(退化成人人可复算,对一杯 ¥9.9 咖啡也够用);配了则外人
/// 无法在开奖前枚举 N 去反推自己中不中。
public final class HiddenBucketDraw {

    private HiddenBucketDraw() {
    }

    /// 从 1..bucketCount 里摇 count 个不重复桶序,升序返回。
    /// bucketCount ≤ 0 → 空;count ≥ bucketCount → 全中(桶比奖还少时人人有份)。
    public static List<Integer> draw(String salt, LocalDate session, int bucketCount, int count) {
        if (bucketCount <= 0 || count <= 0) {
            return List.of();
        }
        List<Integer> pool = new ArrayList<>(bucketCount);
        for (int i = 1; i <= bucketCount; i++) {
            pool.add(i);
        }
        Collections.shuffle(pool, seededRandom(salt, session, bucketCount));
        List<Integer> picked = new ArrayList<>(pool.subList(0, Math.min(count, bucketCount)));
        Collections.sort(picked);
        return List.copyOf(picked);
    }

    /// 种子:SHA-256 摘要前 8 字节拼成 long。用摘要而不是 String.hashCode(),
    /// 是为了让相邻的 N(比如 7 和 8)摇出的结果毫无关联——否则有人能靠"卡桶数"来操纵。
    private static Random seededRandom(String salt, LocalDate session, int bucketCount) {
        String material = (salt == null ? "" : salt) + "|" + session + "|" + bucketCount;
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
        long seed = 0L;
        for (int i = 0; i < 8; i++) {
            seed = (seed << 8) | (digest[i] & 0xFFL);
        }
        // 必须是 new Random(seed):它的线性同余算法由 JDK 规范钉死,跨版本跨机器同种子同结果。
        // 绝不能换成 ThreadLocalRandom / SecureRandom —— 那样重启一次号就变了。
        return new Random(seed);
    }
}
