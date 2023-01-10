package com.bzi.taskcloud.common.exception;

import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.bzi.taskcloud.engine.TaskDispatcherException;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.errors.TransportException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@DecryptRequest
@EncryptResponse
@RestControllerAdvice
public class GlobalExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = IllegalArgumentException.class)
    public Result handler(IllegalArgumentException exception){
        LoggerUtil.failed(exception);

        return Result.failed(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result handler(MethodArgumentNotValidException exception){
        BindingResult bindingResult = exception.getBindingResult();
        ObjectError error = bindingResult.getAllErrors().stream().findFirst().get();

        LoggerUtil.failed(error.getDefaultMessage());

        return Result.failed(error.getDefaultMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = AccessDeniedException.class)
    public void handler(AccessDeniedException exception){
        LoggerUtil.failed("权限校验", exception);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = TaskDispatcherException.class)
    public Result handler(TaskDispatcherException exception){
        LoggerUtil.failed("任务分发失败", exception);

        return Result.failed(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = TransportException.class)
    public Result handler(TransportException exception){
        LoggerUtil.failed("任务仓库导入失败", exception);

        return Result.failed(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = DoNotEncryptionResultException.class)
    @EncryptResponse(value = false)
    public Result handler(DoNotEncryptionResultException exception){
        LoggerUtil.failed("任务仓库导入失败", exception);

        return Result.failed(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = RuntimeException.class)
    public Result handler(RuntimeException exception){
        LoggerUtil.failed("服务器内部错误", exception);

        return Result.failed("服务器内部错误");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = Exception.class)
    public Result handler(Exception exception){
        LoggerUtil.failed("服务器内部错误", exception);

        return Result.failed("服务器内部错误");
    }

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException exception) throws IOException, ServletException {
        LoggerUtil.failed("未经授权", exception);

        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setStatus(401);
        httpServletResponse.getWriter().write(new ObjectMapper().writeValueAsString(
                Result.make(401, "未经授权", null)
        ));
    }

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException exception) throws IOException, ServletException {
        LoggerUtil.failed("错误访问", exception);

        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setStatus(403);
        httpServletResponse.getWriter().write(new ObjectMapper().writeValueAsString(
                Result.make(403, "拒绝服务", null)
        ));
    }
}
