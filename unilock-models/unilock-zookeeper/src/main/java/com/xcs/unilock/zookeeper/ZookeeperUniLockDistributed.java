package com.xcs.unilock.zookeeper;

import com.xcs.unilock.AbstractUniLockDistributed;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ZookeeperDistributedLock 是一个基于 Apache Curator 的分布式锁适配器。
 * 它使用 ZooKeeper 的临时节点来实现分布式环境下的锁机制，确保在多个客户端之间实现资源的互斥访问。
 * 该适配器通过 InterProcessMutex 实现锁的获取和释放，并使用 ThreadLocal
 * 机制为每个线程管理自己的锁实例，确保线程安全和锁的有效管理。
 *
 * @author xcs
 */
public class ZookeeperUniLockDistributed extends AbstractUniLockDistributed<InterProcessMutex> {

    /**
     * 锁的根路径，用于在 ZooKeeper 中存储锁节点。
     */
    private static final String LOCK_ROOT_PATH = "/locks";

    /**
     * CuratorFramework 客户端，用于与 ZooKeeper 交互。
     */
    private final CuratorFramework curatorFramework;

    /**
     * 线程本地变量，存储每个线程的 InterProcessMutex 锁对象。
     * 通过线程本地变量，确保每个线程持有自己的锁实例。
     */
    private final ThreadLocal<Map<String, InterProcessMutex>> zkThreadLocalLocks = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public ZookeeperUniLockDistributed(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public InterProcessMutex doLock(String lockName, String lockValue, long leaseTime, long waitTime) throws Exception {
        // 创建锁路径
        String lockPath = LOCK_ROOT_PATH + "/" + lockName;
        // 获取当前线程的锁集合
        Map<String, InterProcessMutex> locks = zkThreadLocalLocks.get();
        // 获取锁对象，如果不存在则创建新锁对象并存入集合
        InterProcessMutex mutex = locks.computeIfAbsent(lockName, k -> new InterProcessMutex(curatorFramework, lockPath));
        // 尝试在指定时间内获取锁
        if (mutex.acquire(waitTime, TimeUnit.MILLISECONDS)) {
            // 将锁对象存储到线程本地变量中
            locks.put(lockName, mutex);
            return mutex;
        }
        return null;
    }

    @Override
    public void doUnlock(String lockName, String lockValue, InterProcessMutex mutex) throws Exception {
        if (mutex != null) {
            // 释放锁
            mutex.release();
            // 如果当前进程不再持有该锁，则从线程本地变量中移除该锁
            if (!mutex.isAcquiredInThisProcess()) {
                zkThreadLocalLocks.get().remove(lockName);
            }
        }
    }
}
