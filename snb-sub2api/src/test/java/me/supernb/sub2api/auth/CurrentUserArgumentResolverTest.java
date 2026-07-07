package me.supernb.sub2api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.common.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

/// `@CurrentUser` 解析器单测:解析成功返回画像并挂到请求属性(供 JPA 审计取
/// created_by),匿名/非法 token 抛 401 且不挂任何属性。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CurrentUserArgumentResolverTest {

    final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);
    final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver(introspect);

    @Test
    void resolvedProfileIsExposedAsRequestAttribute() {
        UserProfile profile = new UserProfile(42L, "user", "active");
        when(introspect.introspect("Bearer t")).thenReturn(Optional.of(profile));
        NativeWebRequest request = mock(NativeWebRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer t");

        Object resolved = resolver.resolveArgument(null, null, request, null);

        assertThat(resolved).isSameAs(profile);
        verify(request).setAttribute(
                CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE, profile, RequestAttributes.SCOPE_REQUEST);
    }

    @Test
    void anonymousRequestThrowsUnauthorizedAndSetsNoAttribute() {
        when(introspect.introspect(null)).thenReturn(Optional.empty());
        NativeWebRequest request = mock(NativeWebRequest.class);

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, request, null))
                .isInstanceOf(UnauthorizedException.class);
        verify(request, never()).setAttribute(any(), any(), anyInt());
    }
}
