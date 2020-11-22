package com.alone.springhandle.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * @ClassName AloneService
 * @Author zzzzwwwwwwwwwwwwww
 * @Date 2020/11/22 18:33
 * @Description AloneService
 * @Version 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AloneService {
    @AliasFor(
            annotation = Component.class
    )
    String value() default "";
}
