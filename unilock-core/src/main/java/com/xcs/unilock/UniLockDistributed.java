package com.xcs.unilock;

/**
 * DistributedLock
 *
 * @author xcs
 */
public interface UniLockDistributed<T> {

    /**
     * 尝试在指定的时间内获取锁。如果在超时之前可以获取到锁，则返回true；
     * 如果超时仍无法获取到锁，则返回false。
     *
     * @param lockName  锁的名称
     * @param leaseTime 过期时间防止死锁 (ms)
     * @param waitTime  尝试获取锁超时时间 (ms)
     * @return 锁的响应
     */
    UniLockResponse<T> tryLock(String lockName, long leaseTime, long waitTime);

    /**
     * 释放指定名称的锁。
     *
     * @param response 锁的响应
     */
    boolean unlock(UniLockResponse<T> response);

    /**
     * 自定义重入锁
     *
     * @return 是否可重入
     */
    default boolean reentrant() {
        return false;
    }

    /**
     * 自定义锁的续期
     *
     * @return 是否可重入
     */
    default boolean renewal() {
        return false;
    }

    /**
     * 锁续期
     *
     * @param lockName  锁的名称
     * @param lockValue 锁的值
     * @param leaseTime 过期时间防止死锁 (ms)
     */
    default void doRenewal(String lockName, String lockValue, long leaseTime) {
        throw new UnsupportedOperationException("renew expiration expiration not supported");
    }
}
