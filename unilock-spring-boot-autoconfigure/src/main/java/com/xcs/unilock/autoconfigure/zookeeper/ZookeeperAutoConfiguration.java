package com.xcs.unilock.autoconfigure.zookeeper;

import com.xcs.unilock.zookeeper.ZookeeperUniLockDistributed;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Zookeeper自动配置类。
 * 该类负责根据配置条件自动创建Zookeeper分布式锁的相关Bean。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ZookeeperUniLockDistributed.class, CuratorFramework.class})
@EnableConfigurationProperties({ZookeeperLockProperties.class})
public class ZookeeperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ZookeeperLockProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public ZookeeperUniLockDistributed zookeeperDistributedLock(ZookeeperLockProperties properties) {
        Assert.hasText(properties.getConnectString(), "zookeeper connect string must be set.  Use the property: spring.unilock.zookeeper.connectString");
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                properties.getOptions().getBaseSleepTimeMs(),
                properties.getOptions().getMaxRetries(),
                properties.getOptions().getMaxSleepMs()
        );
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(properties.getConnectString())
                .ensembleTracker(properties.getOptions().isWithEnsembleTracker())
                .sessionTimeoutMs(properties.getOptions().getSessionTimeoutMs())
                .connectionTimeoutMs(properties.getOptions().getConnectionTimeoutMs())
                .maxCloseWaitMs(properties.getOptions().getMaxCloseWaitMs())
                .namespace(properties.getOptions().getNamespace())
                .canBeReadOnly(properties.getOptions().isCanBeReadOnly())
                .waitForShutdownTimeoutMs(properties.getOptions().getWaitForShutdownTimeoutMs())
                .simulatedSessionExpirationPercent(properties.getOptions().getSimulatedSessionExpirationPercent())
                .retryPolicy(retryPolicy);
        if (!properties.getOptions().isUseContainerParentsIfAvailable()) {
            builder.dontUseContainerParents();
        }
        return new ZookeeperUniLockDistributed(builder.build());
    }
}
