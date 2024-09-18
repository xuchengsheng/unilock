package com.xcs.unilock.jedis;

import com.xcs.unilock.AbstractDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * JedisDistributedLock 是基于原生 Redis 实现的分布式锁适配器。
 * 利用 Redis 的 SETNX 命令和 Lua 脚本来实现锁的获取、释放和状态查询功能。
 *
 * @author xcs
 */
@SuppressWarnings({"BusyWait", "RedundantSuppression"})
public class JedisDistributedLock extends AbstractDistributedLock {

    /**
     * 日志记录器，用于捕获和记录错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JedisDistributedLock.class);

    /**
     * 锁成功获取的标识
     */
    private static final String LOCK_SUCCESS = "OK";

    /**
     * 锁成功释放的标识
     */
    private static final String RELEASE_SUCCESS = "1";

    /**
     * 使用 Lua 脚本确保只有持有锁的线程才能解锁
     */
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 使用 Lua 脚本确保只有持有锁的线程才能续期
     */
    private static final String RENEWAL_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end";

    /**
     * UnifiedJedis
     */
    private final UnifiedJedis jedis;

    public JedisDistributedLock(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public boolean customReentrant() {
        return true;
    }

    @Override
    public boolean customRenewal() {
        return true;
    }

    @Override
    public boolean doLock(String lockName, String lockValue, long leaseTime, long waitTime) {
        // 设置NX和过期时间
        SetParams setParams = new SetParams().nx().px(TimeUnit.MILLISECONDS.toMillis(leaseTime));
        // 尝试获取锁 SET myLock myValue NX PX 5000
        String result = jedis.set(lockName, lockValue, setParams);
        // 超时未获取到锁
        return LOCK_SUCCESS.equals(result);
    }

    @Override
    public void doUnlock(String lockName, String lockValue) {
        // 执行 Lua 脚本解锁
        Object result = jedis.eval(UNLOCK_SCRIPT, Collections.singletonList(lockName), Collections.singletonList(lockValue));
        // 解锁成功
        if (!RELEASE_SUCCESS.equals(result.toString())) {
            LOGGER.warn("Unlock failed or lock was not held by this client lock: {}", lockName);
        }
    }

    @Override
    public void doRenewal(String lockName, String lockValue, long leaseTime) {
        Object result = jedis.eval(RENEWAL_SCRIPT, Collections.singletonList(lockName), Arrays.asList(lockValue, String.valueOf(leaseTime)));
        if (!RELEASE_SUCCESS.equals(result.toString())) {
            LOGGER.warn("Failed to extend lock expiration time for lock: {}. The lock might have been released or expired.", lockName);
        }
    }
}