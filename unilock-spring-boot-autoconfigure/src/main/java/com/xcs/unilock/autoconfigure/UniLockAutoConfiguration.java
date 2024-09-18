package com.xcs.unilock.autoconfigure;

import com.xcs.unilock.DistributedLock;
import com.xcs.unilock.aop.UniLockAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UniLockAutoConfiguration 是一个自动配置类，负责注册分布式锁的切面（Advisor）。
 * 通过该配置类，可以在 Spring 容器中自动注入与分布式锁相关的 AOP 逻辑。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection"})
public class UniLockAutoConfiguration {

    /**
     * 注册 UniLockAdvisor Bean，用于拦截带有 @UniLock 和 @UniLocks 注解的方法。
     *
     * @param distributedLock 分布式锁的实现，提供锁的获取和释放操作
     * @return UniLockAdvisor 对象，负责处理分布式锁的 AOP 逻辑
     */
    @Bean
    public UniLockAdvisor uniLockAdvisor(DistributedLock distributedLock) {
        return new UniLockAdvisor(distributedLock);
    }
}
