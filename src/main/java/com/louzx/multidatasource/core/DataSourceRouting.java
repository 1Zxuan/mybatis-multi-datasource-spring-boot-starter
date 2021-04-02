package com.louzx.multidatasource.core;

import com.louzx.multidatasource.properteis.MultiDataSourceProperties;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DataSourceRouting extends AbstractRoutingDataSource {

    private final ThreadLocal<String> localDataSourceName = new ThreadLocal<>();

    private final ThreadLocal<Map<String, ConnectionImpl>> connectionThreadLocal = new ThreadLocal<>();

    private final Map<String, DataSource> dataSourceMap = new HashMap<>(4);

    public void add(String dataSourceName, DataSource dataSource) {
        this.dataSourceMap.put(dataSourceName, dataSource);
    }

    public void buildDataSource() {
        setTargetDataSources((Map) dataSourceMap);
    }

    public void change(String value) {
        if (!dataSourceMap.containsKey(value) || null == dataSourceMap.get(value)) {
            throw new RuntimeException(String.format("数据源【%s】不存在", value));
        }
        localDataSourceName.set(value);
    }

    public void remove() {
        localDataSourceName.remove();
    }

    public void close() {
        Map<String, ConnectionImpl> connectionMap = connectionThreadLocal.get();
        if (null != connectionMap) {
            connectionMap.forEach((k, v)-> v.close(true));
        }
        this.connectionThreadLocal.remove();
    }

    public DataSource dateSource(String value) {
        return dataSourceMap.get(value);
    }

    public void bind(String value, Connection connection) {
        Map<String, ConnectionImpl> connectionMap = connectionThreadLocal.get();
        if (null == connectionMap) {
            connectionMap = new HashMap<>();
            connectionThreadLocal.set(connectionMap);
        }
        connectionMap.put(value, new ConnectionImpl(connection));
    }

    public void commit(boolean commit) throws SQLException {
        Map<String, ConnectionImpl> connectionMap = connectionThreadLocal.get();
        if (null != connectionMap) {
            for (Map.Entry<String, ConnectionImpl> entry : connectionMap.entrySet()) {
                entry.getValue().commit(commit);
            }
        }
        this.connectionThreadLocal.remove();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Map<String, ConnectionImpl> connectionMap = connectionThreadLocal.get();
        if (null == connectionMap) {
            return determineTargetDataSource().getConnection();
        } else {
            return connectionMap.get(determineCurrentLookupKey());
        }
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (null == localDataSourceName.get()) {
            if (null != MultiDataSourceProperties.getMasterName()) {
                return MultiDataSourceProperties.getMasterName();
            }
            return dataSourceMap.keySet().iterator().next();
        }

        return localDataSourceName.get();
    }


}
