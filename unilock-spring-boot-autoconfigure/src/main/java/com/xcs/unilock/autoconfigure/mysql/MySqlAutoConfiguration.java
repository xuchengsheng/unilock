package com.xcs.unilock.autoconfigure.mysql;

import com.alibaba.druid.pool.DruidDataSource;
import com.mysql.cj.jdbc.Driver;
import com.xcs.unilock.mysql.MySqlUniLockDistributed;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * MySQL 分布式锁的自动配置类。
 * 该类会在系统中未自定义 MySqlDistributedLock 实例时，自动根据配置属性创建一个。
 * 当配置属性启用并符合要求时，将会实例化 MySqlDistributedLock，并配置相应的数据库连接池。
 * 使用 Druid 数据源和 MySQL JDBC 驱动。
 * 注意：MySQL 的连接属性（URL、用户名、密码等）必须在配置中明确设置。
 *
 * @author xcs
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MySqlUniLockDistributed.class, DruidDataSource.class, Driver.class})
@EnableConfigurationProperties({MySqlLockProperties.class})
public class MySqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = MySqlLockProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public MySqlUniLockDistributed mySqlDistributedLock(MySqlLockProperties properties) {
        Assert.hasText(properties.getUrl(), "mysql url must be set.  Use the property: spring.unilock.mysql.url");
        Assert.hasText(properties.getUsername(), "mysql username must be set.  Use the property: spring.unilock.mysql.username");
        Assert.hasText(properties.getPassword(), "mysql password must be set.  Use the property: spring.unilock.mysql.password");
        Assert.hasText(properties.getDriverClassName(), "mysql driver class name must be set.  Use the property: spring.unilock.mysql.driverClassName");
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setInitialSize(properties.getOptions().getInitialSize());
        dataSource.setMaxActive(properties.getOptions().getMaxActive());
        dataSource.setMinIdle(properties.getOptions().getMinIdle());
        dataSource.setMaxWait(properties.getOptions().getMaxWait());
        return new MySqlUniLockDistributed(dataSource);
    }
}
