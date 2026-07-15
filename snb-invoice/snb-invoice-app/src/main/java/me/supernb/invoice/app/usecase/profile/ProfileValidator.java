package me.supernb.invoice.app.usecase.profile;

import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 抬头字段校验(create/update 共用):title 必填 ≤100;企业税号必填 ≤30;其余选填 ≤200。
final class ProfileValidator {

    private ProfileValidator() {
    }

    static ProfileData validate(ProfileData data) {
        String title = required(data.title(), "抬头名称");
        if (title.length() > 100) {
            throw InvoiceException.invalidInput("抬头名称过长");
        }
        String taxNo = strip(data.taxNo());
        if (data.type() == ProfileType.COMPANY) {
            if (taxNo == null) {
                throw InvoiceException.invalidInput("企业抬头必须填写税号");
            }
            if (taxNo.length() > 30) {
                throw InvoiceException.invalidInput("税号过长");
            }
        }
        return new ProfileData(data.type(), title, taxNo,
                optional(data.regAddress(), "注册地址"), optional(data.regPhone(), "注册电话"),
                optional(data.bankName(), "开户行"), optional(data.bankAccount(), "银行账号"));
    }

    private static String required(String value, String label) {
        String stripped = strip(value);
        if (stripped == null) {
            throw InvoiceException.invalidInput(label + "不能为空");
        }
        return stripped;
    }

    private static String optional(String value, String label) {
        String stripped = strip(value);
        if (stripped != null && stripped.length() > 200) {
            throw InvoiceException.invalidInput(label + "过长");
        }
        return stripped;
    }

    private static String strip(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
