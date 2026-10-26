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

    @ExceptionHandler(Exception.class)
    public Result<Void> error(Exception e) {
        return Result.error(500, e.getMessage() == null ? "服务器内部错误" : e.getMessage());
    }
}
