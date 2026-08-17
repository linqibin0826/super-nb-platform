package me.supernb.ops.app.usecase.query.view;

/// 解密结果(只在「显示密码」端点短暂出现,绝不进列表)。
public record SecretView(String password, String recoveryPassword) {
}
