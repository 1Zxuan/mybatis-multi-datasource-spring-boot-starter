package com.louzx.multidatasource.autoconfig;

import com.louzx.multidatasource.core.DataSourceRouting;
import com.louzx.multidatasource.properteis.DataSourceExtentProperties;
import com.louzx.multidatasource.properteis.MultiDataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceAutoConfig {

    @Resource
    private MultiDataSourceProperties multiDataSourceProperties;

    @Bean("multiDataSource")
    public DataSource multiDataSource() {
        DataSourceRouting dataSourceRouting = new DataSourceRouting();
        Map<String, DataSourceExtentProperties> dataSourcePropertiesMap = multiDataSourceProperties.getDatasource();
        for (String dataSourceName : dataSourcePropertiesMap.keySet()) {
            DataSourceExtentProperties dataSourceProperties = dataSourcePropertiesMap.get(dataSourceName);
            DataSource dataSource = createDataSource(dataSourceProperties);
            dataSourceRouting.add(dataSourceName, dataSource);
            setOtherProperties(dataSource, dataSourceProperties);
        }
        dataSourceRouting.buildDataSource();
        return dataSourceRouting;
    }

    private void setOtherProperties(DataSource dataSource, DataSourceExtentProperties dataSourceProperties) {
        Map<String, Object> pool = dataSourceProperties.getPool();
        if (null != pool) {
            for (Map.Entry<String, Object> entry : pool.entrySet()) {
                Class<? extends DataSource> aClass = dataSource.getClass();
                String fieldName = convert(entry.getKey());
                try {
                    Field field = getField(fieldName, aClass);
                    if (null != field) {
                        field.setAccessible(true);
                        field.set(dataSource, entry.getValue());
                    }
                } catch (IllegalAccessException ignore) {

                }
            }
        }
    }

    private Field getField(String fieldName, Class<? extends DataSource> dataSource) {
        try {
            return dataSource.getField(fieldName);
        } catch (NoSuchFieldException e) {
            //查询父类
            Class<?> superclass = dataSource.getSuperclass();
            try {
                return superclass.getField(fieldName);
            } catch (NoSuchFieldException ignore) {
                return null;
            }
        }
    }

    private DataSource createDataSource (DataSourceProperties dataSourceProperties) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create(dataSourceProperties.getClassLoader())
                .driverClassName(dataSourceProperties.getDriverClassName())
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword());

        if (null != dataSourceProperties.getType()) {
            dataSourceBuilder.type(dataSourceProperties.getType());
        }
        return dataSourceBuilder.build();
    }

    private final Pattern pattern = Pattern.compile("-(\\w)");

    private String convert(String value) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
