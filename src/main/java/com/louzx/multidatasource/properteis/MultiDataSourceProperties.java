package com.louzx.multidatasource.properteis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix="spring.multi")
public class MultiDataSourceProperties {

    Map<String, DataSourceExtentProperties> datasource;

    public Map<String, DataSourceExtentProperties> getDatasource() {
        return datasource;
    }

    public void setDatasource(Map<String, DataSourceExtentProperties> datasource) {
        this.datasource = datasource;
    }

    private static String masterName;

    @Value("${spring.multi.master.name}")
    public void setMasterName(String masterName) {
        MultiDataSourceProperties.masterName = masterName;
    }

    public static String getMasterName() {
        return masterName;
    }
}
