package me.supernb.sub2api.auth;

import me.supernb.common.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/// `@CurrentUser UserProfile` 参数解析器:Authorization 头 → introspect → 要求 active 的
/// user/admin 账号,否则抛 [UnauthorizedException](commons UNAUTHORIZED trait,统一映射 401 problem+json)。
///
/// 解析成功后把画像挂到当前请求属性 [#CURRENT_USER_ATTRIBUTE],
/// 供 JPA 审计的 `AuditorAware`(boot 层装配)取 created_by/updated_by。
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /// 请求属性键,挂载本次请求已鉴权通过的 [UserProfile]。
    public static final String CURRENT_USER_ATTRIBUTE = "me.supernb.sub2api.currentUser";

    private final Sub2apiIntrospectClient introspect;

    /// 构造:注入 sub2api introspect 客户端。
    public CurrentUserArgumentResolver(Sub2apiIntrospectClient introspect) {
        this.introspect = introspect;
    }

    /// 只接管同时标注 `@CurrentUser`、参数类型可赋给 [UserProfile] 的方法参数。
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserProfile.class.isAssignableFrom(parameter.getParameterType());
    }

    /// 解析当前用户:introspect 失败、或账号非 active 的 user/admin,统一抛 401;
    /// 成功则把画像写入请求属性,供审计消费。
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UserProfile profile = introspect.introspect(webRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(UserProfile::isActiveAccount)
                .orElseThrow(UnauthorizedException::new);
        webRequest.setAttribute(CURRENT_USER_ATTRIBUTE, profile, RequestAttributes.SCOPE_REQUEST);
        return profile;
    }
}
