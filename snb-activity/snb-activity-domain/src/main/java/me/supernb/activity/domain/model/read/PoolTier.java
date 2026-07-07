package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 奖池档位余量。
///
/// @param amount    该档位奖金额(元)
/// @param total     该档位奖槽总数
/// @param available 该档位当前可领(未被认领)的奖槽数
public record PoolTier(BigDecimal amount, int total, int available) {
}
