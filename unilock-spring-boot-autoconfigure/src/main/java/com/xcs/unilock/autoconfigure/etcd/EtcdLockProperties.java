package com.xcs.unilock.autoconfigure.etcd;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Etcd锁的配置属性类。
 * 该类用于从配置文件中读取和存储与Etcd锁相关的配置信息。
 *
 * @author xcs
 */
@Data
@ConfigurationProperties(EtcdLockProperties.CONFIG_PREFIX)
public class EtcdLockProperties {

    public static final String CONFIG_PREFIX = "spring.unilock.etcd";

    /**
     * 是否启用Etcd锁。
     * 指定是否启用Etcd锁功能。
     */
    private boolean enabled = true;

    /**
     * Etcd服务器的端点。
     * 用于指定Etcd集群的连接地址，可以是多个地址的逗号分隔列表。
     */
    private String endpoints;

    /**
     * Etcd客户端选项。
     * 用于配置Etcd客户端的详细选项，例如重试策略、连接保持存活等。
     */
    @NestedConfigurationProperty
    private EtcdOptions options = EtcdOptions.builder()
            .retryDelay(500)
            .retryMaxDelay(2500)
            .retryMaxAttempts(2)
            .keepaliveTime(30)
            .keepaliveTimeout(10)
            .keepaliveWithoutCalls(true)
            .waitForReady(true)
            .build();
}
