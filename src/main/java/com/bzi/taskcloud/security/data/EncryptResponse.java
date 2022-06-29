package com.bzi.taskcloud.security.data;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptResponse {
    boolean value() default true;
}
