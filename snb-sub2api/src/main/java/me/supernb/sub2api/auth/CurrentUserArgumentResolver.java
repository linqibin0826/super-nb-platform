package me.supernb.sub2api.auth;

import me.supernb.common.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/// `@CurrentUser UserProfile` 参数解析器:Authorization → introspect →
/// active 的 user/admin 账号,否则抛 401(commons UNAUTHORIZED trait 转 problem+json)。
///
/// 解析成功后同时把画像挂到当前请求属性 [#CURRENT_USER_ATTRIBUTE],
/// 供 JPA 审计的 `AuditorAware`(boot 装配)取 created_by/updated_by。
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /// 请求属性键:本次请求已通过鉴权的 [UserProfile]。
    public static final String CURRENT_USER_ATTRIBUTE = "me.supernb.sub2api.currentUser";

    private final Sub2apiIntrospectClient introspect;

    /// 构造:注入 introspect 客户端。
    public CurrentUserArgumentResolver(Sub2apiIntrospectClient introspect) {
        this.introspect = introspect;
    }

    /// 只接管标注 `@CurrentUser` 的 [UserProfile] 参数。
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserProfile.class.isAssignableFrom(parameter.getParameterType());
    }

    /// 解析当前用户:introspect 失败或非 active 的 user/admin 账号一律 401;
    /// 成功后把画像写入请求属性供审计消费。
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
