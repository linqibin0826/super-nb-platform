package me.supernb.invoice.adapter.rest.request;

import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 抬头入参(type 非法值 → 422)。
public record ProfileInput(String type, String title, String taxNo, String regAddress,
                           String regPhone, String bankName, String bankAccount) {

    /// 转领域数据(字段级校验在 app 层 ProfileValidator)。
    public ProfileData toData() {
        ProfileType profileType;
        try {
            profileType = ProfileType.valueOf(type == null ? "" : type);
        } catch (IllegalArgumentException e) {
            throw InvoiceException.invalidInput("抬头类型必须是 COMPANY 或 PERSONAL");
        }
        return new ProfileData(profileType, title, taxNo, regAddress, regPhone, bankName, bankAccount);
    }
}
