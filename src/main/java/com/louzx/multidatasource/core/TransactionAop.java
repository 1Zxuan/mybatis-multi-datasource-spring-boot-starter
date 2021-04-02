package com.louzx.multidatasource.core;

import com.louzx.multidatasource.annotation.MultiTransaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Aspect
@Component
public class TransactionAop {

    @Resource
    private DataSourceRouting dataSourceRouting;

    @Pointcut("@annotation(com.louzx.multidatasource.annotation.MultiTransaction)")
    public void annotationTransactionPointcut(){
    }

    @Around("annotationTransactionPointcut()")
    public void around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MultiTransaction annotation = signature.getMethod().getAnnotation(MultiTransaction.class);
        if (null != annotation && annotation.values().length > 0) {
            open(annotation.values(), annotation.transactionType());
        }
        joinPoint.proceed();
        dataSourceRouting.commit(true);
    }

    @AfterThrowing("annotationTransactionPointcut()")
    public void afterThrowing() {
        try {
            dataSourceRouting.commit(false);
        } catch (SQLException ignore) {

        }
    }

    private void open(String[] values, int transactionType) throws SQLException {
        for (String value : values) {
            DataSource dataSource = dataSourceRouting.dateSource(value);
            if (null != dataSource) {
                Connection connection = dataSource.getConnection();
                if (connection.getAutoCommit()) {
                    connection.setAutoCommit(false);
                }
                prepareConnectionTransactionType(connection,transactionType);
                dataSourceRouting.bind(value,connection);
            }
        }
    }

    private void prepareConnectionTransactionType(Connection connection, int transactionType) throws SQLException {
        if (TransactionType.checkTransactionType(transactionType)) {
            connection.setTransactionIsolation(transactionType);
        } else {
            throw new SQLException("事物隔离级别错误");
        }
    }

    private enum TransactionType {
        TRANSACTION_NONE(0),
        TRANSACTION_READ_UNCOMMITTED(1),
        TRANSACTION_READ_COMMITTED(2),
        TRANSACTION_REPEATABLE_READ(4),
        TRANSACTION_SERIALIZABLE(8);

        public Integer type;

        TransactionType(Integer type) {
            this.type = type;
        }

        public static boolean checkTransactionType(int transactionType) {
            for (TransactionType tt : TransactionType.values()) {
                if (tt.type == transactionType) {
                    return true;
                }
            }
            return false;
        }
    }
}
