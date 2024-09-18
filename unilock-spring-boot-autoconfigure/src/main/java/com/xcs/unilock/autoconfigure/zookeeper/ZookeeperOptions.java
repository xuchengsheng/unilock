package com.xcs.unilock.autoconfigure.zookeeper;

import lombok.Builder;
import lombok.Data;

/**
 * Zookeeper连接的相关配置选项。
 *
 * @author xcs
 */
@Data
@Builder
public class ZookeeperOptions {

    /**
     * 是否启用EnsembleTracker，用于跟踪Zookeeper集群成员的变化。
     */
    private boolean withEnsembleTracker;

    /**
     * Zookeeper会话的超时时间（以毫秒为单位）。
     */
    private int sessionTimeoutMs;

    /**
     * 与Zookeeper服务器建立连接的超时时间（以毫秒为单位）。
     */
    private int connectionTimeoutMs;

    /**
     * 客户端关闭时最大等待时间（以毫秒为单位）。
     */
    private int maxCloseWaitMs;

    /**
     * Zookeeper的命名空间，用于隔离不同应用的数据。
     */
    private String namespace;

    /**
     * 是否允许客户端在Zookeeper服务器进入只读模式时继续读取数据。
     */
    private boolean canBeReadOnly;

    /**
     * 如果可用，是否使用容器节点作为父节点。
     */
    private boolean useContainerParentsIfAvailable;

    /**
     * 在关闭客户端时，等待会话终止的超时时间（以毫秒为单位）。
     */
    private int waitForShutdownTimeoutMs;

    /**
     * 模拟会话过期的概率百分比，用于测试目的。
     */
    private int simulatedSessionExpirationPercent;

    /**
     * 重试之间等待的初始时间（以毫秒为单位）
     */
    private int baseSleepTimeMs;

    /**
     * 重试的最大次数
     */
    private int maxRetries;

    /**
     *  每次重试时休眠的最大时间（以毫秒为单位）
     */
    private int maxSleepMs;
}
