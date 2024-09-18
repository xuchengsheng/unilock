package com.xcs.unilock.annotation;

import com.xcs.unilock.callback.DefaultLockFailCallback;
import com.xcs.unilock.callback.LockFailCallback;

import java.lang.annotation.*;

/**
 * UniLock 注解用于标记需要使用分布式锁的方法。
 * <p>可以通过注解属性配置锁的条件、名称、过期时间和获取锁的超时时间。</p>
 *
 * @author xcs
 */
@Inherited
@Documented
@Repeatable(UniLocks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface UniLock {

    /**
     * 锁的条件表达式，支持 Spring EL 表达式。
     * <p>当条件满足时，才会尝试获取锁。Spring EL 表达式允许动态地根据方法参数或其他上下文生成锁的条件。</p>
     *
     * @return 条件表达式的字符串
     */
    String condition() default "";

    /**
     * 锁的名称，支持 Spring EL 表达式。
     * <p>用于标识不同的锁。如果未指定，将使用默认的锁名称。默认锁名称由包名、类名和方法名组合而成。</p>
     *
     * @return 锁的名称
     */
    String name() default "";

    /**
     * 锁的过期时间（毫秒）。
     * <p>如果设置为 30000，表示锁将在 30 秒后过期。</p>
     *
     * @return 过期时间（毫秒）
     */
    long leaseTime() default 30000;

    /**
     * 获取锁的超时时间（毫秒）。
     * <p>如果设置为 3000，表示尝试获取锁的最大时间为 3 秒。如果超过此时间未获取到锁，则立即返回。</p>
     *
     * @return 获取锁的超时时间（毫秒）
     */
    long waitTime() default 3000;

    /**
     * 获取锁失败时的回调策略类。
     * <p>回调策略类必须实现 {@link LockFailCallback} 接口，用于定义在获取锁失败时的处理逻辑。</p>
     *
     * @return 回调策略类
     */
    Class<? extends LockFailCallback<?>> onFail() default DefaultLockFailCallback.class;
}

