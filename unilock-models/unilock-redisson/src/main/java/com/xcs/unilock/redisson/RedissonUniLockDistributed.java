package com.xcs.unilock.redisson;

import com.xcs.unilock.AbstractUniLockDistributed;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * RedissonDistributedLock 是一个基于 Redisson 实现的分布式锁适配器。
 * 它利用 Redisson 提供的 RLock 来实现分布式锁的功能。
 *
 * @author xcs
 */
public class RedissonUniLockDistributed extends AbstractUniLockDistributed<RLock> {

    /**
     * 日志记录器，用于捕获和记录错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonUniLockDistributed.class);

    /**
     * Redisson 客户端实例，用于与 Redis 进行交互。
     */
    private final RedissonClient redissonClient;

    /**
     * 构造函数，初始化 RedissonLockAdapter。
     *
     * @param redissonClient Redisson 客户端实例
     */
    public RedissonUniLockDistributed(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RLock doLock(String lockName, String lockValue, long leaseTime, long waitTime) throws Exception {
        // 获取 RLock 对象
        RLock rLock = redissonClient.getLock(lockName);
        // 尝试获取锁，并设置锁的持有时间和超时时间
        boolean locked = rLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        // 如果获取锁成功
        if (locked) {
            // 设置了过期时间，则调用 scheduleExpirationRenewal 方法
            if (leaseTime > 0) {
                overrideParentInternalLockLeaseTime(rLock, TimeUnit.MILLISECONDS.toMillis(leaseTime));
                invokeScheduleExpirationRenewal(rLock);
            }
            return rLock;
        }
        // 返回锁获取结果
        return null;
    }

    @Override
    public void doUnlock(String lockName, String lockValue, RLock rLock) {
        // 仅当当前线程持有锁时，释放锁
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
        }
    }

    /**
     * 修改父类 RedissonBaseLock 的 internalLockLeaseTime 值。
     *
     * @param lock                     RedissonLock 对象
     * @param newInternalLockLeaseTime 新的 internalLockLeaseTime 值
     */
    public void overrideParentInternalLockLeaseTime(RLock lock, long newInternalLockLeaseTime) {
        try {
            // 获取 RedissonBaseLock 的父类，即 RedissonLock 的父类
            Class<?> superclass = lock.getClass().getSuperclass();
            // 获取父类的 internalLockLeaseTime 字段
            Field internalLockLeaseTimeField = superclass.getDeclaredField("internalLockLeaseTime");
            // 设置访问权限
            internalLockLeaseTimeField.setAccessible(true);
            // 修改 internalLockLeaseTime 字段的值
            internalLockLeaseTimeField.set(lock, newInternalLockLeaseTime);
        } catch (Exception e) {
            LOGGER.error("Failed to override internalLockLeaseTime", e);
        }
    }

    /**
     * 使用反射调用 RedissonBaseLock 的 scheduleExpirationRenewal 方法。
     *
     * @param lock RLock 对象
     */
    private void invokeScheduleExpirationRenewal(RLock lock) {
        try {
            // 获取 RedissonBaseLock 的 scheduleExpirationRenewal 方法
            Method scheduleRenewalMethod = lock.getClass().getSuperclass().getDeclaredMethod("scheduleExpirationRenewal", long.class);
            // 设置可以访问 protected 方法
            scheduleRenewalMethod.setAccessible(true);
            // 获取当前线程的 ID
            long threadId = Thread.currentThread().getId();
            // 通过反射调用方法
            scheduleRenewalMethod.invoke(lock, threadId);
        } catch (Exception e) {
            LOGGER.error("Failed to invoke scheduleExpirationRenewal method", e);
        }
    }
}
