package com.kuafuai.config.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.google.common.collect.Maps;
import com.kuafuai.common.util.RandomStringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.dto.RdsDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.db.enable", havingValue = "true")
public class RdsManager {

    private final static String RDS_KEY = "rds:route:list";
    private final static String APP_ROUTE_RDS_KEY = "rds:route:app";

    @Resource
    private DynamicRoutingDataSource routingDataSource;

    private final Map<Object, Object> dataSourceMap = Maps.newConcurrentMap();

    @Resource(name = "dataRouterRedisTemplate")
    private RedisTemplate dataRouterRedisTemplate;

    @Resource
    private DruidProperties druidProperties;

    @Value("${spring.datasource.druid.url}")
    private String defaultJDBCURL;
    @Value("${spring.datasource.druid.username}")
    private String defaultJDBCUSERNAME;
    @Value("${spring.datasource.druid.password}")
    private String defaultJDBCPASSWORD;


    @PostConstruct
    public void init() {

        addRdsIfAbsent("DEFAULT", defaultJDBCURL, defaultJDBCUSERNAME, defaultJDBCPASSWORD);

        List<RdsDTO> rdsList = getRdsList();
        if (CollectionUtils.isEmpty(rdsList)) {
            return;
        }

        rdsList.forEach(dto -> addRdsIfAbsent(dto.getRdsKey(), dto.getUrl(), dto.getUsername(), dto.getPassword()));
    }

    /**
     * 获取 缓存中配置的rds
     */
    public List<RdsDTO> getRdsList() {
        List<RdsDTO> rdsList = dataRouterRedisTemplate.opsForList().range(RDS_KEY, 0, -1);
        rdsList.add(RdsDTO.builder()
                .rdsKey("DEFAULT")
                .priority(2.1)
                .build());

        return rdsList;
    }

    /**
     * 添加一个rds
     */
    public void add(RdsDTO rdsDTO) {
        if (StringUtils.isEmpty(rdsDTO.getRdsKey())) {
            rdsDTO.setRdsKey("RDS_" + RandomStringUtils.generateRandomString(10));
        }
        dataRouterRedisTemplate.opsForList().rightPush(RDS_KEY, rdsDTO);
        addRdsIfAbsent(rdsDTO.getRdsKey(), rdsDTO.getUrl(), rdsDTO.getUsername(), rdsDTO.getPassword());
    }

    /**
     * 获取 appId 对应的 rdsKey
     */
    public String getRdsKeyByAppId(String appId) {
        Object value = dataRouterRedisTemplate.opsForHash().get(APP_ROUTE_RDS_KEY, appId);
        if (Objects.isNull(value)) {
            return "";
        } else {
            return value.toString();
        }
    }

    /**
     * 设置appId对应的 rdsKey
     */
    public void putRdsKeyByAppId(String appId, String rdsKey) {
        dataRouterRedisTemplate.opsForHash().put(APP_ROUTE_RDS_KEY, appId, rdsKey);
    }


    private void addRdsIfAbsent(String rdsKey, String url,
                                String username,
                                String password) {

        if (dataSourceMap.containsKey(rdsKey)) {
            return;
        }
        log.info("=====add rds key :{}=========", rdsKey);
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        druidProperties.dataSource(dataSource);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSourceMap.put(rdsKey, dataSource);

        //设置数据源
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.afterPropertiesSet();
    }


}
