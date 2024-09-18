package com.xcs.unilock.callback;

import org.aopalliance.intercept.MethodInvocation;

/**
 * LockFailCallback 接口定义了获取锁失败时的回调策略。
 * <p>该接口用于在尝试获取分布式锁失败时执行自定义的处理逻辑，并返回一个值作为方法的结果。</p>
 *
 * @param <T> 回调执行后返回的结果类型
 * @author xcs
 */
public interface LockFailCallback<T> {

    /**
     * 当获取锁失败时调用的回调方法。
     * <p>该方法允许在锁获取失败时执行自定义的逻辑。回调方法接收锁的名称和方法调用上下文作为参数，并返回一个值作为方法的最终结果。</p>
     *
     * @param lockName   尝试获取的锁的名称
     * @param invocation 包含方法调用信息的 {@link MethodInvocation} 对象
     * @return 获取锁失败后的返回值
     */
    T onFail(String lockName, MethodInvocation invocation);
}
