package com.kuafuai.api.auth.handler.huoshan;

import com.google.gson.Gson;
import com.kuafuai.api.auth.entity.HuoshanConfig;
import com.kuafuai.api.auth.handler.HuoshanParamProcessor;
import com.kuafuai.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WordToPicProcessor implements HuoshanParamProcessor {

    private final Gson gson = new Gson();

    @Override
    public boolean match(String reqKey) {
        return StringUtils.equalsIgnoreCase(reqKey, "jimeng_t2i_v40");
    }

    @Override
    public void beforeSign(HuoshanConfig config, Map<String, Object> params) {
        if (StringUtils.equalsIgnoreCase(config.getAction(), "CVSync2AsyncGetResult")) {
            Map<String, Object> req = new HashMap<>();
            req.put("return_url", true);
            params.put("req_json", gson.toJson(req));
        }
    }

    @Override
    public void afterSign(HuoshanConfig config, Map<String, Object> params) {
        if (StringUtils.equalsIgnoreCase(config.getAction(), "CVSync2AsyncGetResult")) {
            Map<String, Object> req = new HashMap<>();
            req.put("return_url", true);

            String json = gson.toJson(gson.toJson(req));
            params.put("req_json", json.substring(1, json.length() - 1));
        }
    }

    @Override
    public String template(HuoshanConfig config) {
        if (StringUtils.equalsIgnoreCase(config.getAction(), "CVSync2AsyncGetResult")) {
            return "{\"req_key\":\"jimeng_t2i_v40\",\"task_id\":\"${{task_id}}\",\"req_json\":\"${{req_json}}\"}";
        } else {
            return "{\"req_key\":\"jimeng_t2i_v40\",\"image_urls\":${{image_urls}},\"prompt\":\"${{prompt}}\",\"scale\":${{scale}}}";
        }
    }
}
