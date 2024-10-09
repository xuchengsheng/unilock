package com.xcs.unilock.aop;

import com.xcs.unilock.UniLockDistributed;
import com.xcs.unilock.annotation.UniLocks;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

/**
 * UniLockAdvisor 是一个切面类，用于拦截带有 @UniLock 和 @UniLocks 注解的方法。
 * 它利用 AOP（面向切面编程）机制，在方法执行前后处理分布式锁的获取和释放。
 * 通过组合多个切入点，本类支持在同一个方法上同时使用多个锁，并确保这些锁在方法执行过程中正确获取和释放。
 *
 * @author xcs
 */
@SuppressWarnings({"NullableProblems", "rawtypes"})
public class UniLockAdvisor extends AbstractPointcutAdvisor {

    /**
     * 定义切入点，用于捕捉 @UniLock 和 @UniLocks 注解的方法
     */
    private final Pointcut pointcut;

    /**
     * 定义拦截器，用于处理分布式锁的逻辑
     */
    private final UniLockInterceptor interceptor;

    public UniLockAdvisor(UniLockDistributed uniLockDistributed) {
        // 创建针对 @UniLock 注解的切入点
        Pointcut uniLockPointcut = new AnnotationMatchingPointcut(null, com.xcs.unilock.annotation.UniLock.class);
        // 创建针对 @UniLocks 注解的切入点
        Pointcut uniLocksPointcut = new AnnotationMatchingPointcut(null, UniLocks.class);
        // 组合两个切入点，支持同时匹配 @UniLock 和 @UniLocks 注解
        this.pointcut = new ComposablePointcut(uniLockPointcut).union(uniLocksPointcut);
        // 初始化拦截器
        this.interceptor = new UniLockInterceptor(uniLockDistributed);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return interceptor;
    }
}
