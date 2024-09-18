package com.xcs.unilock.callback;

import com.xcs.unilock.exception.LockAcquisitionFailedException;
import org.aopalliance.intercept.MethodInvocation;

/**
 * DefaultLockFailCallback 是默认的锁获取失败处理策略。
 * <p>当获取锁失败时，该类将抛出 {@link LockAcquisitionFailedException} 自定义异常，以通知调用方锁获取失败。</p>
 *
 * @author xcs
 */
public class DefaultLockFailCallback implements LockFailCallback<Void> {

    @Override
    public Void onFail(String lockName, MethodInvocation invocation) {
        // 获取调用失败的目标方法名称
        String methodName = invocation.getMethod().getName();
        // 抛出锁获取失败的异常，并包含锁的名称和方法名称
        throw new LockAcquisitionFailedException(lockName, methodName);
    }
}
