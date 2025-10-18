package com.kuafuai.manage.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.manage.service.ManageConstants;
import com.kuafuai.system.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class RegisterEventListener {
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ManageBusinessService manageBusinessService;

    @PostConstruct
    public void init() {
        //注册订阅者
        eventBus.register(this);
    }

    @Subscribe
    public void handleEvent(EventVo event) {
        if (!validateEvent(event)) {
            return;
        }
        String email = event.getTableName();
        Users current = manageBusinessService.getByEmail(email);
        if (current == null) {
            return;
        }
        manageBusinessService.createApp("Example App", current.getId());
    }

    private boolean validateEvent(EventVo event) {
        if (event == null) {
            return false;
        }

        if (!StringUtils.equalsIgnoreCase(event.getModel(), ManageConstants.EVENT_REGISTER)) {
            // 只处理注册事件
            return false;
        }

        if (StringUtils.isEmpty(event.getTableName())) {
            log.warn("注册事件缺少邮箱字段，忽略");
            return false;
        }

        return true;
    }
}
