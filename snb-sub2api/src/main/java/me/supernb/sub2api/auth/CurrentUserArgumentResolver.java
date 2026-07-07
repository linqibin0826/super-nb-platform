package me.supernb.sub2api.auth;

import me.supernb.common.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/// @CurrentUser 参数解析器:与旧 controller 手写 requireUserId 完全同语义——
/// 转发 Authorization 头 introspect,只放行 active 终端用户,其余一律 401(commons 统一转 problem+json)。
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final Sub2apiIntrospectClient introspect;

    /// 构造:注入 introspect 客户端。
    public CurrentUserArgumentResolver(Sub2apiIntrospectClient introspect) {
        this.introspect = introspect;
    }

    /// 只处理标注 @CurrentUser 的 UserProfile 参数。
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserProfile.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return introspect.introspect(webRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(UserProfile::isActiveUser)
                .orElseThrow(UnauthorizedException::new);
    }
}
