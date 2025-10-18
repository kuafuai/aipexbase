package com.kuafuai.api.controller;

import com.kuafuai.api.service.TextBusinessService;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/text")
public class TextController {

    @Autowired
    private TextBusinessService textBusinessService;

    @PostMapping("/text2text")
    public BaseResponse text2text(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("text参数不存在，请检查参数！");
        }

        String text = Convert.toStr(data.get("text"));
        if (StringUtils.isEmpty(text)) {
            return ResultUtils.error("text参数不能为空，请检查参数！");
        }

        String appId = GlobalAppIdFilter.getAppId();

        String prompt = String.valueOf(data.getOrDefault("prompt", ""));
        String conversationId = String.valueOf(data.getOrDefault("conversationId", ""));

        String content = textBusinessService.text2text(appId, text, prompt, conversationId);
        return ResultUtils.success(content);
    }

    @PostMapping("/tts")
    public BaseResponse tts(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("text参数不存在，请检查参数！");
        }
        String appId = GlobalAppIdFilter.getAppId();
        String text = String.valueOf(data.get("text"));
        String content = textBusinessService.tts(appId, text);
        return ResultUtils.success(content);
    }
}
