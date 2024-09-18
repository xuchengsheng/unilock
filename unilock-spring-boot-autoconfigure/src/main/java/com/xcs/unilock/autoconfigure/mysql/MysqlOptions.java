package com.xcs.unilock.autoconfigure.mysql;

import lombok.Builder;
import lombok.Data;

/**
 * MySQL连接池的配置选项类。
 *
 * @author xcs
 */
@Data
@Builder
public class MysqlOptions {

    /**
     * 初始连接数。
     * 连接池在初始化时创建的连接数。
     */
    protected int initialSize;

    /**
     * 最大活动连接数。
     * 连接池中可以同时分配的最大连接数。
     */
    protected int maxActive;

    /**
     * 最小空闲连接数。
     * 连接池中保持的最小空闲连接数。
     */
    protected int minIdle;

    /**
     * 最大等待时间。
     * 当连接池中的连接已用尽时，等待获取连接的最大时间（单位：毫秒）。
     */
    protected long maxWait;
}
