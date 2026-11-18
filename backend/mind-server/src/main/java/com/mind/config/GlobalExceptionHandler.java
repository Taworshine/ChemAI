package com.mind.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.mind.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：
 *  - 登录失效（NotLoginException）→ HTTP 401，前端拦截器据此跳回登录页
 *    （之前直接抛 500，前端只处理 401，导致用户看到一堆 "Request failed with status code 500"）
 *  - 其他未捕获异常 → 500 + 简短信息，避免长堆栈直出前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> notLogin(NotLoginException e) {
        return Result.error(401, "登录已失效，请重新登录");
    }

    /** 权限类异常统一返回 403（业务校验抛出的 SecurityException），避免误报 500 */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> forbidden(SecurityException e) {
        return Result.error(403, e.getMessage() == null ? "无权执行此操作" : e.getMessage());
    }

    /** 参数类异常统一返回 400 */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> badRequest(RuntimeException e) {
        return Result.error(400, e.getMessage() == null ? "请求不合法" : e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> error(Exception e) {
        return Result.error(500, e.getMessage() == null ? "服务器内部错误" : e.getMessage());
    }
}
