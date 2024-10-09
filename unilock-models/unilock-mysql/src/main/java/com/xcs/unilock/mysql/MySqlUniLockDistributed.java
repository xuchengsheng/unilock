package com.xcs.unilock.mysql;

import com.alibaba.druid.pool.DruidDataSource;
import com.xcs.unilock.AbstractUniLockDistributed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * MySqlDistributedLock 是基于 MySQL 实现的分布式锁适配器。
 * 利用 MySQL 数据库的行级锁来实现锁的获取、释放和状态查询功能。
 * 通过设置锁的过期时间来防止死锁问题，使锁在特定时间后自动失效。
 *
 * @author xcs
 */
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve", "BusyWait", "deprecation", "RedundantSuppression"})
public class MySqlUniLockDistributed extends AbstractUniLockDistributed<String> {

    /**
     * 日志记录器，用于捕获和记录错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlUniLockDistributed.class);

    /**
     * SQL 语句：创建表结构
     */
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS distributed_locks (lock_name VARCHAR(255) NOT NULL,locked_by VARCHAR(255) NOT NULL,lock_time TIMESTAMP NOT NULL,expire_time TIMESTAMP NOT NULL,PRIMARY KEY (lock_name),INDEX idx_expire_time (expire_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    /**
     * SQL 查询语句，用于在数据库中检查指定锁是否存在，并获取其过期时间。
     */
    private static final String TRY_SELECT_SQL = "SELECT lock_name, expire_time FROM distributed_locks WHERE lock_name = ? FOR UPDATE";

    /**
     * SQL 插入语句，用于在数据库中插入新锁记录。
     * 如果锁已经存在，则更新锁的持有者和过期时间。
     */
    private static final String TRY_INSERT_SQL = "INSERT INTO distributed_locks (lock_name, locked_by, lock_time, expire_time) VALUES (?, ?, ?, ?)";

    /**
     * SQL 删除语句，用于删除已经过期的锁记录。
     */
    private static final String DELETE_EXPIRED_LOCK_SQL = "DELETE FROM distributed_locks WHERE lock_name = ? AND expire_time < ?";

    /**
     * SQL 删除语句，用于释放锁，即从数据库中删除锁记录。
     */
    private static final String UN_LOCK_DELETE_SQL = "DELETE FROM distributed_locks WHERE lock_name = ? AND locked_by = ?";

    /**
     * SQL 更新语句，用于延长锁的过期时间。
     */
    private static final String UPDATE_EXPIRE_TIME_SQL = "UPDATE distributed_locks SET expire_time = ? WHERE lock_name = ? AND locked_by = ?";

    /**
     * 数据库连接对象，用于执行 SQL 操作。
     */
    private final DruidDataSource dataSource;

    /**
     * 当前节点的标识符，用于区分不同节点持有的锁。
     */
    private final String nodeId;

    /**
     * 构造函数，初始化 MySqlLockAdapter 实例。
     *
     * @param dataSource 数据源
     */
    public MySqlUniLockDistributed(DruidDataSource dataSource) {
        this.dataSource = dataSource;
        this.nodeId = getNodeId();
        createTableIfNotExists();
    }

    @Override
    public boolean reentrant() {
        return true;
    }

    @Override
    public boolean renewal() {
        return true;
    }

    @Override
    public String doLock(String lockName, String lockValue, long leaseTime, long waitTime) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(TRY_SELECT_SQL);
            stmt.setString(1, lockName);
            // 查询锁的当前状态
            try (ResultSet rs = stmt.executeQuery()) {
                boolean canMysqlLock = true;
                // 如果锁存在，检查是否过期
                if (rs.next()) {
                    Timestamp expireTime = rs.getTimestamp("expire_time");
                    // 如果锁已过期，尝试删除并获取锁
                    if (expireTime != null && expireTime.toInstant().isBefore(Instant.now())) {
                        // 删除过期的锁记录
                        deleteExpiredLock(connection, lockName);
                    } else {
                        // 如果锁存在且未过期，则不能获取锁
                        canMysqlLock = false;
                    }
                }
                // 如果可以获取锁，则插入一条记录
                return String.valueOf(canMysqlLock && insertLock(connection, lockName, leaseTime));
            }
        }
    }

    @Override
    public void doUnlock(String lockName, String lockValue, String instance) throws Exception {
        // 从数据库中删除锁记录
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(UN_LOCK_DELETE_SQL);
            stmt.setString(1, lockName);
            stmt.setString(2, nodeId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void doRenewal(String lockName, String lockValue, long leaseTime) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(UPDATE_EXPIRE_TIME_SQL);
            stmt.setTimestamp(1, Timestamp.from(Instant.now().plusSeconds(TimeUnit.MILLISECONDS.toSeconds(leaseTime))));
            stmt.setString(2, lockName);
            stmt.setString(3, nodeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to extend lock expiration time for lock: {}", lockName, e);
        }
    }

    /**
     * 在数据库中创建表结构（如果不存在）。
     */
    private void createTableIfNotExists() {
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            LOGGER.error("Failed to create table structure for distributed locks", e);
        }
    }

    /**
     * 插入锁记录的具体实现。
     *
     * @param connection 数据库连接对象
     * @param lockName   锁的名称
     * @param leaseTime  锁的过期时间
     * @return boolean 返回是否插入成功
     */
    private boolean insertLock(Connection connection, String lockName, long leaseTime) {
        try (PreparedStatement insertStmt = connection.prepareStatement(TRY_INSERT_SQL)) {
            insertStmt.setString(1, lockName);
            insertStmt.setString(2, nodeId);
            insertStmt.setTimestamp(3, Timestamp.from(Instant.now()));
            insertStmt.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(TimeUnit.MILLISECONDS.toSeconds(leaseTime))));
            // 如果插入成功，说明获取锁成功
            return insertStmt.executeUpdate() > 0;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * 删除过期的锁记录。
     *
     * @param connection 数据库连接对象
     * @param lockName   锁的名称
     */
    private void deleteExpiredLock(Connection connection, String lockName) {
        // 删除过期的锁记录
        try (PreparedStatement deleteStmt = connection.prepareStatement(DELETE_EXPIRED_LOCK_SQL)) {
            deleteStmt.setString(1, lockName);
            deleteStmt.setTimestamp(2, Timestamp.from(Instant.now()));
            deleteStmt.executeUpdate();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 获取当前节点的ID。
     *
     * @return 当前节点的ID
     */
    private String getNodeId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            return "Unknown";
        }
    }
}
