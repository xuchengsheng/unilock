package com.xcs.unilock.autoconfigure.mysql;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * MySQL锁的属性配置类。
 *
 * @author xcs
 */
@Data
@ConfigurationProperties(MySqlLockProperties.CONFIG_PREFIX)
public class MySqlLockProperties {

    public static final String CONFIG_PREFIX = "spring.unilock.mysql";

    /**
     * 是否启用MySQL锁。
     * 如果设置为true，则启用MySQL锁功能。
     */
    private boolean enabled = true;

    /**
     * 数据库连接的URL。
     * 用于指定连接到MySQL数据库的URL。
     */
    private String url;

    /**
     * 数据库连接的用户名。
     * 用于指定连接到MySQL数据库时使用的用户名。
     */
    private String username;

    /**
     * 数据库连接的密码。
     * 用于指定连接到MySQL数据库时使用的密码。
     */
    private String password;

    /**
     * 数据库驱动类的名称。
     * 用于指定连接到MySQL数据库时使用的JDBC驱动类。
     */
    private String driverClassName;

    /**
     * MySQL连接池的配置选项。
     * 包含与连接池相关的属性，如初始连接数、最大活动连接数等。
     */
    @NestedConfigurationProperty
    private MysqlOptions options = MysqlOptions.builder()
            .initialSize(0)
            .maxActive(8)
            .minIdle(0)
            .maxWait(-1)
            .build();
}
