package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 初始化拦截器
     */
    @PostConstruct
    public void init() {
        log.info("JwtTokenAdminInterceptor 初始化完成");
        log.info("Token名称配置为：{}", jwtProperties.getAdminTokenName());
    }

    /**
     * 校验jwt
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return 是否放行
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.info("进入拦截器 preHandle 方法");
        log.info("请求URI: {}", request.getRequestURI());
        log.info("请求方法: {}", request.getMethod());

        // 判断当前拦截到的是 Controller 的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            log.info("非动态方法，直接放行，handler类型：{}", handler.getClass());
            return true;
        }

        // 1. 从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        if (token == null || token.isEmpty()) {
            log.warn("请求头中未包含令牌: {}", jwtProperties.getAdminTokenName());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 状态码
            return false;
        }

        // 2. 校验令牌
        try {
            log.info("开始校验 jwt: {}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);

            // 从令牌中提取员工 ID 并记录日志
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("JWT 校验通过，当前员工 ID: {}", empId);
            BaseContext.setCurrentId(empId);
            // 令牌校验通过后，可以将员工 ID 放入请求的 Attribute，便于后续使用
            request.setAttribute(JwtClaimsConstant.EMP_ID, empId);

            // 校验成功，放行请求
            return true;
        } catch (Exception ex) {
            // 校验失败，记录日志并响应 401 状态码
            log.error("JWT 校验失败: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 状态码
            return false;
        }
    }
}
