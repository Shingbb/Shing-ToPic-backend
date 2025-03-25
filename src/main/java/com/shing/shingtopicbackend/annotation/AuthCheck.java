package com.shing.shingtopicbackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于方法级别的注解，旨在检查方法调用者的权限
 * 该注解主要用于标记需要进行权限验证的方法，通过检查调用者是否具备指定的角色来决定是否允许访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具有某个角色
     * 指定方法调用者必须具备的角色，默认为空字符串，表示不作任何角色限制
     * 如果指定了具体角色，权限检查时会验证调用者是否具有该角色
     **/
    String mustRole() default "";
}
