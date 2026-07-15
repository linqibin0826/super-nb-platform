package me.supernb.invoice.domain.model.read;

import me.supernb.invoice.domain.model.ProfileType;

/// 抬头列表行。
public record ProfileView(long id, ProfileType type, String title, String taxNo, String regAddress,
                          String regPhone, String bankName, String bankAccount) {
}
