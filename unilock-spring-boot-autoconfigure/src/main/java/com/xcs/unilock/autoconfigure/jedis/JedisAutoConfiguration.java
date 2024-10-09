package com.xcs.unilock.autoconfigure.jedis;

import com.xcs.unilock.jedis.JedisUniLockDistributed;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.*;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 分布式锁的自动配置类。
 * 该类根据不同的 Redis 配置模式（单节点、哨兵、集群）自动配置 Jedis 分布式锁。
 * 如果配置启用且未自定义 JedisDistributedLock 实例，则会自动注入。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({JedisUniLockDistributed.class, JedisPool.class})
@EnableConfigurationProperties({JedisLockProperties.class})
public class JedisAutoConfiguration {

    /**
     * 根据配置属性创建 JedisDistributedLock 实例。
     * 优先尝试配置哨兵模式，其次为集群模式，最后为单节点模式。
     *
     * @param properties Redis 锁的配置属性。
     * @return JedisDistributedLock 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = JedisLockProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public JedisUniLockDistributed jedisDistributedLock(JedisLockProperties properties) {
        UnifiedJedis jedis = Optional.ofNullable(getSentinelConfig(properties))
                .orElse(Optional.ofNullable(getClusterConfig(properties))
                        .orElse(getStandaloneConfig(properties)));
        return new JedisUniLockDistributed(jedis);
    }

    /**
     * 获取单节点模式的 Jedis 配置。
     *
     * @param properties Redis 锁的配置属性。
     * @return 返回 JedisPooled 实例。
     */
    private UnifiedJedis getStandaloneConfig(JedisLockProperties properties) {
        GenericObjectPoolConfig<Connection> poolConfig = getPoolConfig(properties.getOptions());
        return new JedisPooled(poolConfig, properties.getHost(), properties.getPort(), properties.getConnectionTimeout(),
                properties.getTimeout(), properties.getPassword(),
                properties.getDatabase(), properties.getClientName());
    }

    /**
     * 获取哨兵模式的 Jedis 配置。
     *
     * @param properties Redis 锁的配置属性。
     * @return 如果存在哨兵配置，返回 JedisSentineled 实例，否则返回 null。
     */
    private UnifiedJedis getSentinelConfig(JedisLockProperties properties) {
        JedisLockProperties.Sentinel sentinel = properties.getSentinel();
        if (sentinel == null) {
            return null;
        }
        GenericObjectPoolConfig<Connection> poolConfig = getPoolConfig(properties.getOptions());

        Set<HostAndPort> sentinels = sentinel.getNodes().stream()
                .map(HostAndPort::from).collect(Collectors.toSet());

        DefaultJedisClientConfig config = DefaultJedisClientConfig.create(properties.getConnectionTimeout(), properties.getTimeout(),
                0, null, properties.getPassword(), properties.getDatabase(),
                properties.getClientName(), false, null, null, null, null);

        return new JedisSentineled(sentinel.getMaster(), config, poolConfig, sentinels, config);
    }

    /**
     * 获取集群模式的 Jedis 配置。
     *
     * @param properties Redis 锁的配置属性。
     * @return 如果存在集群配置，返回 JedisCluster 实例，否则返回 null。
     */
    private UnifiedJedis getClusterConfig(JedisLockProperties properties) {
        JedisLockProperties.Cluster cluster = properties.getCluster();
        if (cluster == null) {
            return null;
        }
        GenericObjectPoolConfig<Connection> poolConfig = getPoolConfig(properties.getOptions());

        Set<HostAndPort> clusterNodes = cluster.getNodes().stream()
                .map(HostAndPort::from).collect(Collectors.toSet());

        int redirects = cluster.getMaxRedirects() != null ? cluster.getMaxRedirects() : 5;

        return new JedisCluster(clusterNodes, properties.getConnectionTimeout(), properties.getTimeout(), redirects,
                properties.getPassword(), properties.getClientName(), poolConfig);
    }

    /**
     * 获取Redis连接池的配置。
     *
     * @param options Redis连接池的配置选项
     * @return 配置好的GenericObjectPoolConfig实例
     */
    private GenericObjectPoolConfig<Connection> getPoolConfig(JedisOptions options) {
        GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(options.getMaxTotal());
        config.setMaxIdle(options.getMaxIdle());
        config.setMinIdle(options.getMinIdle());
        config.setBlockWhenExhausted(options.isBlockWhenExhausted());
        config.setMaxWait(Duration.ofMillis(options.getMaxWaitMillis()));
        config.setTestOnBorrow(options.isTestOnBorrow());
        config.setTestOnReturn(options.isTestOnReturn());
        config.setJmxEnabled(options.isJmxEnabled());
        config.setTestWhileIdle(options.isTestWhileIdle());
        config.setTimeBetweenEvictionRuns(Duration.ofMillis(options.getTimeBetweenEvictionRunsMillis()));
        config.setMinEvictableIdleDuration(Duration.ofMillis(options.getMinEvictableIdleTimeMillis()));
        config.setNumTestsPerEvictionRun(options.getNumTestsPerEvictionRun());
        return config;
    }
}
