package com.guoyu.platform.common.exception;

import java.sql.SQLException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * Detects MySQL deadlock / lock-wait failures that are safe to retry in a new transaction.
 */
public final class SqlLockContention {

  public static final int MYSQL_DEADLOCK = 1213;
  public static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;
  public static final String SQLSTATE_SERIALIZATION_FAILURE = "40001";

  private SqlLockContention() {
  }

  public static boolean isRetryable(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof DeadlockLoserDataAccessException
          || current instanceof CannotAcquireLockException
          || current instanceof PessimisticLockingFailureException) {
        return true;
      }
      if (current instanceof SQLException sql) {
        if (SQLSTATE_SERIALIZATION_FAILURE.equals(sql.getSQLState())
            || sql.getErrorCode() == MYSQL_DEADLOCK
            || sql.getErrorCode() == MYSQL_LOCK_WAIT_TIMEOUT) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }
}
