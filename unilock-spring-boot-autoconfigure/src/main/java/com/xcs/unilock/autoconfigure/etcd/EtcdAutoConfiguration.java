package com.xcs.unilock.autoconfigure.etcd;

import com.xcs.unilock.etcd.EtcdUniLockDistributed;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * EtcdAutoConfiguration 是 Etcd 分布式锁的自动配置类。
 * 通过读取配置文件中的属性，自动创建并配置 Etcd 分布式锁。
 * 该类的作用是当项目中存在 EtcdDistributedLock 和 Client 类时，根据属性值
 * 动态注入 EtcdDistributedLock 对象，并提供相应的连接和配置参数。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({EtcdUniLockDistributed.class, Client.class})
@EnableConfigurationProperties({EtcdLockProperties.class})
public class EtcdAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = EtcdLockProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public EtcdUniLockDistributed etcdDistributedLock(EtcdLockProperties properties) {
        Assert.hasText(properties.getEndpoints(), "etcd endpoints must be set.  Use the property: spring.unilock.redisson.endpoints");
        EtcdOptions options = properties.getOptions();
        ClientBuilder clientBuilder = Client.builder().endpoints(properties.getEndpoints().split(","));
        if (options.getUser() != null) {
            clientBuilder.user(ByteSequence.from(options.getUser().getBytes()));
        }
        if (options.getPassword() != null) {
            clientBuilder.password(ByteSequence.from(options.getPassword().getBytes()));
        }
        if (options.getLoadBalancerPolicy() != null) {
            clientBuilder.loadBalancerPolicy(options.getLoadBalancerPolicy());
        }
        if (options.getAuthority() != null) {
            clientBuilder.authority(options.getAuthority());
        }
        if (options.getNamespace() != null) {
            clientBuilder.namespace(ByteSequence.from(options.getNamespace().getBytes()));
        }
        if (options.getRetryMaxDuration() > 0) {
            clientBuilder.retryMaxDuration(Duration.ofSeconds(options.getRetryMaxDuration()));
        }
        if (options.getConnectTimeout() > 0) {
            clientBuilder.connectTimeout(Duration.ofSeconds(options.getConnectTimeout()));
        }
        clientBuilder.maxInboundMessageSize(options.getMaxInboundMessageSize())
                .retryDelay(options.getRetryDelay())
                .retryMaxDelay(options.getRetryMaxDelay())
                .retryMaxAttempts(options.getRetryMaxAttempts())
                .keepaliveTime(Duration.ofSeconds(options.getKeepaliveTime()))
                .keepaliveTimeout(Duration.ofSeconds(options.getKeepaliveTimeout()))
                .keepaliveWithoutCalls(options.isKeepaliveWithoutCalls())
                .waitForReady(options.isWaitForReady());
        return new EtcdUniLockDistributed(clientBuilder.build());
    }
}
