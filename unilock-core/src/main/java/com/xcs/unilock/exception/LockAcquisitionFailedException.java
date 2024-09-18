package com.xcs.unilock.exception;

/**
 * LockAcquisitionFailedException 是自定义的运行时异常，
 * 用于表示获取分布式锁失败的异常情况，提供锁名称和方法信息。
 *
 * @author xcs
 */
public class LockAcquisitionFailedException extends RuntimeException {

    /**
     * 构造函数，接受锁名称和方法信息作为异常消息。
     *
     * @param lockName   锁的名称。
     * @param methodName 方法的名称。
     */
    public LockAcquisitionFailedException(String lockName, String methodName) {
        super("Failed to acquire lock. Lock name: " + lockName + ", Method name: " + methodName);
    }
}
