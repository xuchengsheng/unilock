package com.xcs.unilock.etcd;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * EtcdHolder 是锁的持有者对象，包含锁的路径和租约ID。
 */
@Data
@AllArgsConstructor
public class EtcdHolder {
    /**
     * Etcd 中锁的路径。
     */
    private String key;

    /**
     * Etcd 中锁的租约ID。
     */
    private long leaseId;
}
