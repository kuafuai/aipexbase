package com.kuafuai.config.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Objects;

@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        Object key = DynamicDataSourceContextHolder.getDataSourceType();
        log.info("=================== current lookup key ============ {}", key);
        if (Objects.isNull(key)) {
            return "DEFAULT";
        } else {
            return key;
        }
    }
}
