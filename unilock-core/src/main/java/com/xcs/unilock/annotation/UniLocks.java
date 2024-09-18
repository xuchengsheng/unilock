package com.xcs.unilock.annotation;

import java.lang.annotation.*;

/**
 * UniLocks 注解用于组合多个 {@link UniLock} 注解。
 * <p>该注解允许在同一个方法上同时应用多个分布式锁，实现对多个资源的并发控制。</p>
 *
 * @author xcs
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface UniLocks {

    /**
     * 包含多个 {@link UniLock} 注解的数组。
     * <p>每个 {@link UniLock} 注解代表一个独立的锁配置，允许对多个锁进行并发控制。</p>
     *
     * @return 包含的多个锁配置
     */
    UniLock[] value();
}
