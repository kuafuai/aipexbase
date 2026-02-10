package com.kuafuai.api.auth.handler.huoshan;

import com.kuafuai.api.auth.entity.HuoshanConfig;
import com.kuafuai.api.auth.handler.HuoshanParamProcessor;
import com.kuafuai.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WordToVideoProcessor implements HuoshanParamProcessor {
    @Override
    public boolean match(String reqKey) {
        return StringUtils.equalsIgnoreCase(reqKey, "jimeng_t2v_v30");
    }

    @Override
    public void beforeSign(HuoshanConfig config, Map<String, Object> params) {

    }

    @Override
    public void afterSign(HuoshanConfig config, Map<String, Object> params) {

    }

    @Override
    public String template(HuoshanConfig config) {
        if (StringUtils.equalsIgnoreCase(config.getAction(), "CVSync2AsyncGetResult")) {
            return "{\"req_key\":\"jimeng_t2v_v30\",\"task_id\":\"${{task_id}}\"}";
        } else {
            return "{\"req_key\":\"jimeng_t2v_v30\",\"prompt\":\"${{prompt}}\"}";
        }
    }
}
