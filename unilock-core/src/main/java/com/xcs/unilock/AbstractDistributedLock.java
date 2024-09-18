package com.xcs.unilock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象分布式锁类，提供了分布式锁的基本实现。
 *
 * <p>该类定义了获取和释放分布式锁的基本方法，并支持锁的续期。具体的分布式锁实现（如基于 Redis、Zookeeper 等）需要继承此类并实现具体的锁逻辑。</p>
 *
 * @author xcs
 */
public abstract class AbstractDistributedLock implements DistributedLock {

    /**
     * 日志记录器，用于捕获和记录错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDistributedLock.class);

    /**
     * 线程本地变量，存储每个线程持有的锁及其持有计数。
     *
     * <p>每个线程的锁上下文以锁名称为键，持有计数的原子整数为值。</p>
     */
    private final ThreadLocal<Map<String, AtomicInteger>> lockCountHolder = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 定时调度线程池，用于定期延长锁的过期时间。
     *
     * <p>使用一个固定大小的线程池来执行锁续期任务。</p>
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * 存储锁名称与续期任务的映射。
     *
     * <p>用于在锁释放时取消相应的续期任务。</p>
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> lockRenewalTasks = new ConcurrentHashMap<>();

    /**
     * 执行锁的获取操作。
     *
     * <p>该方法由具体的分布式锁实现提供，执行获取锁的具体逻辑。</p>
     *
     * @param lockName  锁的名称
     * @param lockValue 锁的值
     * @param leaseTime 锁的过期时间（毫秒）
     * @param waitTime  尝试获取锁的超时时间（毫秒）
     * @return 如果成功获取锁，则返回 {@code true}；否则返回 {@code false}
     * @throws Exception 在获取锁过程中可能抛出的异常
     */
    public abstract boolean doLock(String lockName, String lockValue, long leaseTime, long waitTime) throws Exception;

    /**
     * 执行锁的释放操作。
     *
     * <p>该方法由具体的分布式锁实现提供，执行释放锁的具体逻辑。</p>
     *
     * @param lockName  锁的名称
     * @param lockValue 锁的值
     * @throws Exception 在释放锁过程中可能抛出的异常
     */
    public abstract void doUnlock(String lockName, String lockValue) throws Exception;

    @Override
    public boolean tryLock(String lockName, long leaseTime, long waitTime) {
        boolean customReentrant = customReentrant();
        // 自定义重入锁
        if (customReentrant) {
            Map<String, AtomicInteger> locks = lockCountHolder.get();
            // 如果当前线程已经持有该锁，则直接返回成功响应
            if (locks.containsKey(lockName)) {
                // 当前线程已经持有该锁，计数器加1
                locks.get(lockName).incrementAndGet();
                // 返回表示锁已经成功获取的响应对象
                return true;
            }
        }
        // 将超时时间转换为毫秒
        long timeoutMillis = TimeUnit.MILLISECONDS.toMillis(waitTime);
        // 记录开始尝试获取锁的时间
        long startTime = System.currentTimeMillis();
        // 用于标识锁持有者
        String lockValue = UUID.randomUUID().toString();
        // 锁已经存在，等待一段时间后重试
        do {
            try {
                // 执行锁的获取
                if (doLock(lockName, lockValue, leaseTime, waitTime)) {
                    // 则将锁上下文存储到当前线程的本地变量中
                    if (customReentrant) {
                        lockCountHolder.get().put(lockName, new AtomicInteger(1));
                    }
                    // 如果支持锁续期，则启动一个定时任务来延长锁的过期时间
                    if (customRenewal()) {
                        scheduleExpirationRenewal(lockName, lockValue, leaseTime);
                    }
                    return true;
                }
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (Exception e) {
                LOGGER.error("Failed to acquire lock: {}", lockName, e);
            }
        } while (System.currentTimeMillis() - startTime < timeoutMillis);
        // 获取锁失败
        return false;
    }

    @Override
    public boolean unlock(String lockName) {
        if (customReentrant()) {
            // 获取当前线程持有的锁
            Map<String, AtomicInteger> locks = lockCountHolder.get();
            // 当前线程没有持有该锁，抛出异常
            if (!locks.containsKey(lockName)) {
                throw new IllegalMonitorStateException("Current thread does not hold the lock: " + lockName);
            }
            // 减少持有锁的计数器
            int holdCount = locks.get(lockName).decrementAndGet();
            // 如果计数器变为 0，表示当前线程已经完全释放了锁
            if (holdCount == 0) {
                // 锁完全释放，从线程本地变量中移除
                locks.remove(lockName);
            } else {
                return true;
            }
        }
        // 执行锁的释放
        try {
            // 如果支持锁续期，则取消定时任务
            if (customRenewal()) {
                // 取消定时任务
                ScheduledFuture<?> task = lockRenewalTasks.remove(lockName);
                if (task != null) {
                    task.cancel(false);
                }
            }
            doUnlock(lockName, "");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 启动定时任务来定期延长锁的过期时间。
     *
     * <p>计算锁过期时间的三分之一，并以此为间隔启动定时任务。</p>
     *
     * @param lockName  锁的名称
     * @param lockValue 锁的值
     * @param leaseTime 锁的过期时间（毫秒）
     */
    private void scheduleExpirationRenewal(String lockName, String lockValue, long leaseTime) {
        // 计算锁过期时间的2/3
        long delay = leaseTime / 3;
        // 启动定时任务
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> doRenewal(lockName, lockValue, leaseTime), delay, delay, TimeUnit.MILLISECONDS);
        // 将定时任务存储到映射中，以便在锁释放时可以取消
        lockRenewalTasks.put(lockName, scheduledFuture);
    }
}
