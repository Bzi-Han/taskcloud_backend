package com.bzi.taskcloud.security.data;

import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@ControllerAdvice
public class DataAdvice implements RequestBodyAdvice, ResponseBodyAdvice<Result> {
    @Value("${data-security.data.decrypt-enable}")
    private boolean decrypt;
    @Value("${data-security.data.encrypt-enable}")
    private boolean encrypt;

    private final ObjectMapper objectMapper;

    public DataAdvice() {
        objectMapper = new ObjectMapper();

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

        objectMapper.registerModule(javaTimeModule);
    }

    // 解密请求体
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return decrypt;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (!isNeedDecrypt(parameter))
            return inputMessage;

        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(
                        Objects.requireNonNull(
                                DataCrypto.decrypt(
                                        new String(inputMessage.getBody().readAllBytes())
                                )
                        ).getBytes(StandardCharsets.UTF_8)
                );
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }


    // 加密返回结果
    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return encrypt;
    }

    @Override
    public Result beforeBodyWrite(Result result, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (Objects.isNull(result))
            return null;
        if (!isNeedEncrypt(methodParameter))
            return result;

        try {
            var encryptResult = DataCrypto.encrypt(objectMapper.writeValueAsString(result));

            serverHttpResponse.getBody().write(encryptResult.getBytes(StandardCharsets.UTF_8));
            serverHttpResponse.getBody().flush();
        } catch (Exception e) {
            LoggerUtil.failed("返回加密结果失败", e);
        }

        return null;
    }


    // 判断是否需要加解密
    private boolean isNeedEncrypt(MethodParameter methodParameter) {
        boolean result;
        boolean classPresentAnnotation = methodParameter.getContainingClass().isAnnotationPresent(EncryptResponse.class);
        boolean methodPresentAnnotation = Objects.requireNonNull(methodParameter.getMethod()).isAnnotationPresent((EncryptResponse.class));

        // 如果类不包含注解则不需要加密
        if (!classPresentAnnotation)
            return false;

        result = methodParameter.getContainingClass().getAnnotation(EncryptResponse.class).value();
        if (!result) //如果注解值为false则不需要加密
            return false;

        // 不设置默认类全体函数加密，设置可单独控制具体方法是否加密
        if (methodPresentAnnotation) {
            result = methodParameter.getMethod().getAnnotation(EncryptResponse.class).value();
        }

        return result;
    }

    private boolean isNeedDecrypt(MethodParameter methodParameter) {
        boolean result;
        boolean classPresentAnnotation = methodParameter.getContainingClass().isAnnotationPresent(DecryptRequest.class);
        boolean methodPresentAnnotation = Objects.requireNonNull(methodParameter.getMethod()).isAnnotationPresent((DecryptRequest.class));

        // 如果类不包含注解则不需要解密
        if (!classPresentAnnotation)
            return false;

        result = methodParameter.getContainingClass().getAnnotation(DecryptRequest.class).value();
        if (!result) //如果注解值为false则不需要解密
            return false;

        // 不设置默认类全体函数解密，设置可单独控制具体方法是否解密
        if (methodPresentAnnotation) {
            result = methodParameter.getMethod().getAnnotation(DecryptRequest.class).value();
        }

        return result;
    }

}
