package com.xcs.unilock.aop;


import com.xcs.unilock.UniLockDistributed;
import com.xcs.unilock.UniLockResponse;
import com.xcs.unilock.callback.LockFailCallback;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * UniLockInterceptor 拦截器用于在方法调用前后处理分布式锁的获取与释放。
 * 它基于 @UniLock 注解，结合 AOP 和 Spring EL 表达式，来实现灵活的锁机制。
 *
 * @author xcs
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UniLockInterceptor implements MethodInterceptor {

    /**
     * 日志记录器，用于捕获和记录错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniLockInterceptor.class);

    /**
     * 分布式锁的实例，用于操作锁的获取与释放
     */
    private final UniLockDistributed uniLockDistributed;

    /**
     * 表达式解析器，用于解析和评估 Spring EL 表达式
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名称发现器，用于获取方法参数的名称
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public UniLockInterceptor(UniLockDistributed uniLockDistributed) {
        this.uniLockDistributed = uniLockDistributed;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 获取被拦截的方法
        Method method = invocation.getMethod();
        // 锁的响应结果
        List<UniLockResponse<?>> responses = new ArrayList<>();
        try {
            // 获取方法上的所有 @UniLock 注解
            for (com.xcs.unilock.annotation.UniLock uniLock : AnnotatedElementUtils.findMergedRepeatableAnnotations(method, com.xcs.unilock.annotation.UniLock.class)) {
                // 解析 condition 条件表达式
                String condition = uniLock.condition();
                // 如果条件表达式不为空且评估结果为 false，则跳过锁的获取，直接执行方法
                if (StringUtils.hasText(condition) && Boolean.FALSE.equals(evaluateCondition(condition, method, invocation.getArguments()))) {
                    return invocation.proceed();
                }
                // 获取锁的名称，若未指定则自动生成
                String lockName = getLockName(uniLock.name(), method, invocation.getArguments());
                UniLockResponse<?> response = this.uniLockDistributed.tryLock(lockName, uniLock.leaseTime(), uniLock.waitTime());
                // 如果获取锁失败
                if (response == null) {
                    // 通过反射创建实例
                    LockFailCallback callback = BeanUtils.instantiateClass(uniLock.onFail());
                    // 执行回调方法
                    Object callbackResult = callback.onFail(lockName, invocation);
                    // 检查回调的返回值类型是否与被拦截方法的返回值类型一致
                    if (callbackResult != null && !method.getReturnType().isInstance(callbackResult)) {
                        throw new IllegalStateException(String.format("Lock failure callback return type mismatch. Expected: %s, but got: %s from callback.",
                                method.getReturnType().getName(), callbackResult.getClass().getName()));
                    }
                    return callbackResult;
                }
                responses.add(response);
            }
            // 成功获取锁后，执行目标方法
            return invocation.proceed();
        } finally {
            // 逐一释放所有锁
            for (UniLockResponse<?> response : responses) {
                if (!uniLockDistributed.unlock(response)) {
                    LOGGER.warn("Failed to unlock: {}", response.getLockName());
                }
            }
        }
    }

    /**
     * 评估给定的条件表达式，用于决定是否尝试获取锁。
     *
     * @param condition 条件表达式（支持 Spring EL）
     * @param method    当前被调用的方法
     * @param args      方法的参数
     * @return 条件表达式的评估结果，true 表示满足条件
     */
    private Boolean evaluateCondition(String condition, Method method, Object[] args) {
        // 创建评估上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 获取方法参数的名称
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        // 将参数名和参数值添加到评估上下文中
        if (paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        // 解析并评估条件表达式
        return parser.parseExpression(condition).getValue(context, Boolean.class);
    }

    /**
     * 解析锁的名称，支持 SpEL 表达式。
     *
     * @param lockNameExpression 注解中指定的锁名称（可能是 SpEL 表达式）
     * @param method             当前被调用的方法
     * @param args               方法的参数
     * @return 解析后的锁名称
     */
    private String getLockName(String lockNameExpression, Method method, Object[] args) {
        if (StringUtils.hasText(lockNameExpression)) {
            // 判断是否是 SpEL 表达式（以 '#' 开头）
            if (lockNameExpression.contains("#")) {
                // 创建 SpEL 上下文
                StandardEvaluationContext context = new StandardEvaluationContext();
                String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
                if (paramNames != null) {
                    for (int i = 0; i < args.length; i++) {
                        context.setVariable(paramNames[i], args[i]);
                    }
                }
                // 使用 SpEL 解析锁的名称
                return parser.parseExpression(lockNameExpression).getValue(context, String.class);
            } else {
                // 如果 lockNameExpression 不是 SpEL 表达式，直接返回作为锁名
                return lockNameExpression;
            }
        } else {
            // 如果没有指定 name 属性，则使用默认的包名+类名+方法名作为锁名
            String packageName = method.getDeclaringClass().getPackage().getName();
            String className = method.getDeclaringClass().getSimpleName();
            String methodName = method.getName();
            return packageName + "." + className + "." + methodName;
        }
    }
}
