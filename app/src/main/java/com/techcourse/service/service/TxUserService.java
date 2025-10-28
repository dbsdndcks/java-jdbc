package com.techcourse.service.service;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.datasource.DataSourceUtils;
import com.interface21.transaction.support.TransactionSynchronizationManager;
import com.techcourse.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TxUserService implements UserService {

    private final Logger log = LoggerFactory.getLogger(TxUserService.class);

    private final DataSource dataSource;
    private final UserService userService;

    public TxUserService(final DataSource dataSource, final UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    @Override
    public User findById(final long id) {
        return userService.findById(id);
    }

    @Override
    public void insert(final User user) {
        userService.insert(user);
    }

    @Override
    public void changePassword(final long id, final String newPassword, final String createBy) {
        Connection connection = null;

        try {
            connection = DataSourceUtils.getConnection(dataSource);
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
                throw new DataAccessException("트랜잭션 실행 전 오류가 발생했습니다.");
            }
            TransactionSynchronizationManager.bindResource(dataSource, connection);

            userService.changePassword(id, newPassword, createBy);

            connection.commit();
        } catch (SQLException e) {
            rollbackSafely(connection);
            throw new DataAccessException("트랜잭션 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            rollbackSafely(connection);
            throw new DataAccessException(e);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
            TransactionSynchronizationManager.unbindResource(dataSource);
        }
    }

    private void rollbackSafely(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
    }
}
