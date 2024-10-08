package com.xcs.unilock.etcd;

import com.xcs.unilock.AbstractUniLockDistributed;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * EtcdDistributedLock 是基于 Etcd 实现的分布式锁适配器。
 * 利用 Etcd 的租约机制和键值操作，实现了锁的获取、释放以及状态管理。
 * 该实现支持重入锁，即允许同一线程多次获取同一把锁。
 *
 * @author xcs
 */
public class EtcdUniLockDistributed extends AbstractUniLockDistributed<EtcdHolder> {

    /**
     * Etcd 锁客户端，用于执行分布式锁的相关操作。
     */
    private final Lock lockClient;

    /**
     * Etcd 中的租约客户端，用于管理租约（lease），以保证锁的自动续约功能。
     */
    private final Lease leaseClient;

    public EtcdUniLockDistributed(Client client) {
        this.lockClient = client.getLockClient();
        this.leaseClient = client.getLeaseClient();
    }

    @Override
    public boolean reentrant() {
        return true;
    }

    @Override
    public EtcdHolder doLock(String lockName, String lockValue, long leaseTime, long waitTime) throws Exception {
        ByteSequence lockKey = ByteSequence.from(lockName.getBytes());
        // 将传入的时间转换为秒，并设置租约的存活时间（TTL）
        long leaseTtl = TimeUnit.MILLISECONDS.toSeconds(leaseTime);
        // 创建租约并获取租约ID
        long leaseId = leaseClient.grant(leaseTtl).get().getID();
        // 设置租约自动续约
        leaseClient.keepAlive(leaseId, null);
        // 尝试获取锁，并将锁绑定到租约上
        LockResponse lockResponse = lockClient.lock(lockKey, leaseId).get(waitTime, TimeUnit.MILLISECONDS);
        // 如果成功获取锁
        if (lockResponse != null) {
            String key = lockResponse.getKey().toString(StandardCharsets.UTF_8);
            return new EtcdHolder(key, leaseId);
        }
        // 如果锁未能获取，返回失败的响应对象
        return null;
    }

    @Override
    public void doUnlock(String lockName, String lockValue, EtcdHolder etcdHolder) throws Exception {
        // 如果锁对象存在
        if (etcdHolder != null) {
            // 释放锁
            lockClient.unlock(ByteSequence.from(etcdHolder.getKey().getBytes())).get();
            // 撤销租约以停止自动续约
            leaseClient.revoke(etcdHolder.getLeaseId()).get();
        }
    }
}
