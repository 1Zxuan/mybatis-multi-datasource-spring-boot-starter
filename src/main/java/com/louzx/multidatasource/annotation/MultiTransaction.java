package com.louzx.multidatasource.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MultiTransaction {

    String[] values();

    int transactionType() default Connection.TRANSACTION_READ_UNCOMMITTED;
}
