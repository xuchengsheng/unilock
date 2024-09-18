package com.xcs.unilock.autoconfigure.redisson;

import com.xcs.unilock.redisson.RedissonDistributedLock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redisson 分布式锁的自动配置类。
 * 该类根据不同的 Redis 部署模式（单节点、哨兵模式、集群模式）来自动配置并创建 Redisson 分布式锁。
 * 当用户在配置文件中启用了 Redis 分布式锁，并且未手动定义 RedissonDistributedLock 实例时，
 * 将会自动使用此类创建并配置分布式锁实例。
 * 使用 Redisson 客户端进行 Redis 操作。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RedissonDistributedLock.class, RedissonClient.class})
@EnableConfigurationProperties({RedissonLockProperties.class})
public class RedissonAutoConfiguration {

    /**
     * Redis 和 Redis 安全协议前缀
     */
    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    /**
     * 创建 RedissonDistributedLock 实例。
     * 按优先级顺序尝试不同的 Redis 配置模式（哨兵模式 -> 集群模式 -> 单节点模式），
     * 并根据相应的配置创建锁实例。
     *
     * @param properties Redisson 分布式锁的配置属性
     * @return 配置好的 RedissonDistributedLock 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = RedissonLockProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonDistributedLock redissonDistributedLock(RedissonLockProperties properties) {
        Config config = Optional.ofNullable(getSentinelConfig(properties))
                .orElse(Optional.ofNullable(getClusterConfig(properties))
                        .orElse(getStandaloneConfig(properties)));
        return new RedissonDistributedLock(Redisson.create(config));
    }

    /**
     * 获取 Redis 单节点模式的配置。
     *
     * @param properties Redisson 分布式锁的配置属性
     * @return 配置好的单节点模式的 Redisson Config 实例
     */
    private Config getStandaloneConfig(RedissonLockProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(REDIS_PROTOCOL_PREFIX + properties.getHost() + ":" + properties.getPort())
                .setDatabase(properties.getDatabase())
                .setConnectTimeout(properties.getConnectionTimeout())
                .setTimeout(properties.getTimeout())
                .setClientName(properties.getClientName())
                .setPassword(properties.getPassword());
        return config;
    }

    /**
     * 获取 Redis 哨兵模式的配置。
     *
     * @param properties Redisson 分布式锁的配置属性
     * @return 配置好的哨兵模式的 Redisson Config 实例，如果未设置哨兵配置则返回 null
     */
    private Config getSentinelConfig(RedissonLockProperties properties) {
        RedissonLockProperties.Sentinel sentinel = properties.getSentinel();
        if (sentinel == null) {
            return null;
        }
        String[] nodes = convert(sentinel.getNodes());
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName(sentinel.getMaster())
                .addSentinelAddress(nodes)
                .setDatabase(properties.getDatabase())
                .setConnectTimeout(properties.getConnectionTimeout())
                .setTimeout(properties.getTimeout())
                .setClientName(properties.getClientName())
                .setPassword(properties.getPassword());
        return config;
    }

    /**
     * 获取 Redis 集群模式的配置。
     *
     * @param properties Redisson 分布式锁的配置属性
     * @return 配置好的集群模式的 Redisson Config 实例，如果未设置集群配置则返回 null
     */
    private Config getClusterConfig(RedissonLockProperties properties) {
        RedissonLockProperties.Cluster cluster = properties.getCluster();
        if (cluster == null) {
            return null;
        }
        String[] nodes = convert(cluster.getNodes());
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(nodes)
                .setConnectTimeout(properties.getConnectionTimeout())
                .setTimeout(properties.getTimeout())
                .setClientName(properties.getClientName())
                .setPassword(properties.getPassword());
        return config;
    }

    /**
     * 将节点地址列表转换为带有 Redis 协议前缀的字符串数组。
     *
     * @param nodesObject 原始的节点地址列表
     * @return 转换后的节点地址数组
     */
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    private String[] convert(List<String> nodesObject) {
        List<String> nodes = new ArrayList<>(nodesObject.size());
        for (String node : nodesObject) {
            // 添加 Redis 或 Rediss 前缀，确保地址格式正确
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(REDIS_PROTOCOL_PREFIX + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[nodes.size()]);
    }
}
