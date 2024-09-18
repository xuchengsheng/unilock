package com.xcs.unilock.autoconfigure.etcd;

import lombok.Builder;
import lombok.Data;

/**
 * Etcd客户端的配置选项类。
 *
 * @author xcs
 */
@Data
@Builder
public class EtcdOptions {

    /**
     * 连接Etcd时使用的用户名。
     */
    private String user;

    /**
     * 连接Etcd时使用的密码。
     */
    private String password;

    /**
     * 负载均衡策略。
     * 用于指定Etcd客户端的负载均衡策略。
     */
    private String loadBalancerPolicy;

    /**
     * Etcd集群的授权信息。
     * 用于指定连接Etcd集群时使用的授权信息。
     */
    private String authority;

    /**
     * 最大入站消息大小。
     * 用于限制客户端能够接收的单个消息的最大大小，单位为字节。
     */
    private Integer maxInboundMessageSize;

    /**
     * Etcd命名空间。
     * 用于指定在Etcd中使用的命名空间前缀，避免与其他应用的键冲突。
     */
    private String namespace;

    /**
     * 重试延迟时间。
     * 用于指定Etcd客户端在重试操作时的初始延迟时间，单位为毫秒。
     */
    private long retryDelay;

    /**
     * 最大重试延迟时间。
     * 用于指定Etcd客户端在重试操作时的最大延迟时间，单位为毫秒。
     */
    private long retryMaxDelay;

    /**
     * 最大重试尝试次数。
     * 用于限制Etcd客户端在失败后重试操作的最大次数。
     */
    private int retryMaxAttempts;

    /**
     * 保持存活的时间。
     * 用于指定在保持连接存活时客户端发送心跳的时间间隔，单位为毫秒。
     */
    private long keepaliveTime;

    /**
     * 保持存活超时时间。
     * 用于指定在保持连接存活时等待心跳响应的超时时间，单位为毫秒。
     */
    private long keepaliveTimeout;

    /**
     * 无调用时是否保持存活。
     * 用于指定在没有活跃调用时是否继续发送心跳保持连接存活。
     */
    private boolean keepaliveWithoutCalls;

    /**
     * 重试操作的最大持续时间。
     * 用于限制Etcd客户端重试操作的总持续时间，单位为毫秒。
     */
    private long retryMaxDuration;

    /**
     * 连接超时时间。
     * 用于指定Etcd客户端连接到服务器的超时时间，单位为毫秒。
     */
    private long connectTimeout;

    /**
     * 等待准备就绪。
     * 用于指定在Etcd服务器准备就绪之前是否等待连接。
     */
    private boolean waitForReady;
}
