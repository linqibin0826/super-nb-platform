package me.supernb.content.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/// admin 前缀 token 门：`X-Admin-Token` 必须等于 env 注入的 `content.admin-token`。
///
/// fail-closed：配置缺省（空值）时一律 401，宁可发布管线报错也不许 admin 面裸奔。
/// 公网侧另有 Caddy 站点块对 `/content/v1/admin/*` 直接 404（双保险，本 filter 服务于隧道直连口）。
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private final String adminToken;

    /// 构造：接收 env 注入的 admin token（`CONTENT_ADMIN_TOKEN`）。
    public AdminTokenFilter(@Value("${content.admin-token:}") String adminToken) {
        this.adminToken = adminToken;
    }

    /// 非 admin 前缀直接放行；admin 前缀校验 token（常数时间比较防时序侧信道），不过则 401 断链。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/content/v1/admin/")) {
            chain.doFilter(request, response);
            return;
        }
        String given = request.getHeader("X-Admin-Token");
        boolean ok = !adminToken.isBlank() && given != null
                && MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8),
                        given.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
