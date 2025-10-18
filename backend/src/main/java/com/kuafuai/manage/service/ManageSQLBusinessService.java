package com.kuafuai.manage.service;

import com.google.common.collect.Lists;
import com.kuafuai.system.entity.AppRequirementSQL;
import com.kuafuai.system.mapper.DatabaseMapper;
import com.kuafuai.system.service.AppRequirementSQLService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ManageSQLBusinessService {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000L;

    @Autowired
    private DatabaseMapper databaseMapper;
    @Autowired
    private AppRequirementSQLService appRequirementSQLService;

    public boolean execute(String appId, String sqlContent) {
        boolean flag = _execute(Lists.newArrayList(sqlContent));
        if (flag) {
            AppRequirementSQL appRequirementSQL = AppRequirementSQL.builder()
                    .appId(appId)
                    .requirementId("0")
                    .content(sqlContent)
                    .dslContent("{}")
                    .build();

            return appRequirementSQLService.save(appRequirementSQL);
        } else {
            return false;
        }
    }

    public boolean execute(String appId, List<String> sqlLines) {
        boolean flag = _execute(sqlLines);
        if (flag) {
            String sqlContent = String.join("\n", sqlLines);
            AppRequirementSQL appRequirementSQL = AppRequirementSQL.builder()
                    .appId(appId)
                    .requirementId("0")
                    .content(sqlContent)
                    .dslContent("{}")
                    .build();

            return appRequirementSQLService.save(appRequirementSQL);
        } else {
            return false;
        }
    }


    private boolean _execute(List<String> sqlLines) {
        String sqlContent = String.join("\n", sqlLines);

        for (int retry = 0; retry <= MAX_RETRY_COUNT; retry++) {
            try {
                databaseMapper.createDDL(sqlContent);
                return true;
            } catch (Exception e) {
                log.error("SQL execution failed, retry {}/{}", retry, MAX_RETRY_COUNT, e);
                sleepWithInterruptHandling(BASE_RETRY_DELAY_MS * (retry + 1));
            }
        }
        return false;
    }

    private void sleepWithInterruptHandling(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted during retry delay", e);
        }
    }
}
