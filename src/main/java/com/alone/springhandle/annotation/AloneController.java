package com.alone.springhandle.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.*;

/**
 * @ClassName AloneController
 * @Author zzzzwwwwwwwwwwwwww
 * @Date 2020/11/22 18:32
 * @Description AloneController
 * @Version 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface AloneController {
    @AliasFor(
            annotation = Controller.class
    )
    String value() default "";
}
