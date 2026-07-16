package me.supernb.invoice.domain.model.read;

import java.time.Instant;
import me.supernb.invoice.domain.model.ProfileType;

/// 抬头列表行(verifiedAt=核验章,null 即未核验)。
public record ProfileView(long id, ProfileType type, String title, String taxNo, String regAddress,
                          String regPhone, String bankName, String bankAccount, Instant verifiedAt) {
}
