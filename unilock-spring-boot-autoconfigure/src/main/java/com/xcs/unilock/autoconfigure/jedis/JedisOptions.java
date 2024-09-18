package com.xcs.unilock.autoconfigure.jedis;

import lombok.Builder;
import lombok.Data;

/**
 * Redis连接池配置选项类。
 *
 * @author xcs
 */
@Data
@Builder
public class JedisOptions {

    /**
     * 资源池中的最大连接数。
     */
    private int maxTotal;

    /**
     * 资源池允许的最大空闲连接数。
     */
    private int maxIdle;

    /**
     * 资源池确保的最少空闲连接数。
     */
    private int minIdle;

    /**
     * 当资源池用尽后,调用者是否要等待。只有当值为true时，下面的maxWaitMillis才会生效。
     */
    private boolean blockWhenExhausted;

    /**
     * 当资源池连接用尽后，调用者的最大等待时间(单位为毫秒)。
     */
    private long maxWaitMillis;

    /**
     * 向资源池借用连接时是否做连接有效性检测(ping)，检测到的无效连接将会被移除。
     */
    private boolean testOnBorrow;

    /**
     * 向资源池归还连接时是否做连接有效性检测(ping)，检测到无效连接将会被移除。
     */
    private boolean testOnReturn;

    /**
     * 是否开启JMX监控。
     */
    private boolean jmxEnabled;

    /**
     * 是否在空闲资源监测时通过ping命令监测连接有效性，无效连接将被销毁。
     */
    private boolean testWhileIdle;

    /**
     * 空闲资源的检测周期(单位为毫秒)。
     */
    private long timeBetweenEvictionRunsMillis;

    /**
     * 资源池中资源的最小空闲时间(单位为毫秒)，达到此值后空闲资源将被移除。
     */
    private long minEvictableIdleTimeMillis;

    /**
     * 做空闲资源检测时，每次检测资源的个数。
     */
    private int numTestsPerEvictionRun;
}
