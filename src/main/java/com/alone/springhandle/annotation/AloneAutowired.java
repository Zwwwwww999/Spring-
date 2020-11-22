package com.alone.springhandle.annotation;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.*;

/**
 * @ClassName AloneAutowried
 * @Author zzzzwwwwwwwwwwwwww
 * @Date 2020/11/22 19:46
 * @Description AloneAutowried
 * @Version 1.0
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AloneAutowired {
    boolean required() default true;

    String value() default "";
}
