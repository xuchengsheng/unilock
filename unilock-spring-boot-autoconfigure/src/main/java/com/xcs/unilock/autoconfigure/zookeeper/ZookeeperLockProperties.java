package com.xcs.unilock.autoconfigure.zookeeper;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Zookeeper锁的属性配置类。
 * 该类用于配置Zookeeper连接的基本信息和相关选项，
 * 如是否启用Zookeeper锁、连接字符串以及其他Zookeeper连接的详细配置选项。
 *
 * @author xcs
 */
@Data
@ConfigurationProperties(ZookeeperLockProperties.CONFIG_PREFIX)
public class ZookeeperLockProperties {

    public static final String CONFIG_PREFIX = "spring.unilock.zookeeper";

    /**
     * 是否启用Zookeeper锁。
     */
    private boolean enabled = true;

    /**
     * Zookeeper服务器的连接字符串，用于指定Zookeeper集群的地址。
     */
    private String connectString;

    /**
     * Zookeeper连接的详细选项配置。
     */
    @NestedConfigurationProperty
    private ZookeeperOptions options = ZookeeperOptions.builder()
            .withEnsembleTracker(true)
            .sessionTimeoutMs(60 * 1000)
            .connectionTimeoutMs(15 * 1000)
            .maxCloseWaitMs(1000)
            .canBeReadOnly(false)
            .useContainerParentsIfAvailable(true)
            .waitForShutdownTimeoutMs(0)
            .simulatedSessionExpirationPercent(100)
            .baseSleepTimeMs(1000)
            .maxRetries(3)
            .maxSleepMs(5000)
            .build();
}