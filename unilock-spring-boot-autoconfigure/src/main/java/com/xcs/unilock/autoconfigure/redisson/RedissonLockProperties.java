package com.xcs.unilock.autoconfigure.redisson;

import com.xcs.unilock.autoconfigure.jedis.JedisOptions;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

/**
 * Redisson 锁的属性配置类。
 *
 * @author xcs
 */
@Data
@ConfigurationProperties(RedissonLockProperties.CONFIG_PREFIX)
public class RedissonLockProperties {

    public static final String CONFIG_PREFIX = "spring.unilock.redisson";

    /**
     * 是否启用Redis锁。
     */
    private boolean enabled = true;

    /**
     * 指定Redis服务器的主机名或IP地址。
     */
    private String host = "localhost";

    /**
     * 指定连接到Redis服务器时所使用的端口号。
     */
    private int port = 6379;

    /**
     * 指定连接到Redis服务器时所使用的密码。
     */
    private String password;

    /**
     * 指定连接到哪个Redis数据库。Redis默认支持16个数据库，编号从0到15。
     */
    private int database = 0;

    /**
     * 用于配置 Redis 操作超时的属性。这个属性指定了在进行 Redis 命令执行（如 SET、GET 等）时应该等待的最长时间。如果在指定的时间内操作没有完成，那么命令将会超时并返回一个错误。
     */
    private int timeout = 0;

    /**
     * 连接到 Redis 服务器的超时时间的属性。这个属性指定了应用程序在尝试与 Redis 服务器建立连接时应该等待的最长时间。
     */
    private int connectionTimeout = 10000;

    /**
     * 指定 Redis 客户端的名称。
     */
    private String clientName;

    /**
     * Redis哨兵模式
     */
    private Sentinel sentinel;

    /**
     * Redis集群模式。
     */
    private Cluster cluster;

    /**
     * Redis options.
     */
    @NestedConfigurationProperty
    private JedisOptions options = JedisOptions.builder()
            .maxTotal(8)
            .maxIdle(8)
            .minIdle(0)
            .blockWhenExhausted(true)
            .maxWaitMillis(-1)
            .testOnBorrow(false)
            .testOnReturn(false)
            .jmxEnabled(true)
            .testWhileIdle(false)
            .timeBetweenEvictionRunsMillis(-1)
            .minEvictableIdleTimeMillis(1800000)
            .numTestsPerEvictionRun(3)
            .build();

    /**
     * Redis哨兵模式的相关属性配置。
     */
    @Data
    public static class Sentinel {

        /**
         * Redis主服务器的名称。
         */
        private String master;

        /**
         * 逗号分隔的"host:port"对列表，用于指定哨兵节点。
         */
        private List<String> nodes;

        /**
         * 用于与哨兵节点进行身份验证的密码。
         */
        private String password;
    }

    /**
     * Redis集群模式的相关属性配置。
     */
    @Data
    public static class Cluster {

        /**
         * 逗号分隔的"host:port"对列表，用于启动时连接的初始集群节点列表。
         * 此列表至少需要包含一个节点。
         */
        private List<String> nodes;

        /**
         * 在执行跨集群的命令时，最大重定向的次数。
         */
        private Integer maxRedirects;
    }
}
