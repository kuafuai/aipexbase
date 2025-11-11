package com.kuafuai.api.service;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.parser.ApiResultParser;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.common.util.IdUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class TextBusinessService {

    @Autowired
    private ApiBusinessService apiBusinessService;

    @Resource
    private StorageService storageService;

    /**
     * Ai 文本生成文本
     *
     * @param text
     * @return
     */
    public String text2text(String appId, String text, String prompt, String conversationId) {
        try {
            String keyName = "text2text";
            Map<String, Object> params = Maps.newHashMap();
            params.put("query", text);
            Map<String, Object> inputs = Maps.newHashMap();
            inputs.put("prompt", StringUtils.isNotEmpty(prompt) ? prompt : "用户与你的对话可能只会进行一次,之后也不会和你再次进行对话,也可能进行多轮对话，你需要尽可能的使用用户提供的信息,完成用户的需求,并且使用合理的语言表述出来,step by step");
            params.put("inputs", inputs);
            params.put("response_mode", "blocking");
            params.put("conversation_id", conversationId);
            params.put("user", IdUtils.simpleUUID());

            String result = apiBusinessService.callApi(appId, keyName, params);
            DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, keyName);
            if (StringUtils.isEmpty(setting.getDataType())) {
                ApiResultParser.parser(result);
                return JsonPath.read(result, "$.data.answer");
            } else {
                return JsonPath.read(result, "$.answer");
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    public String tts(String appId, String text) {
        try {
            String keyName = "tts";
            Map<String, Object> params = Maps.newHashMap();
//            params.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
            params.put("text", text);

            String result = apiBusinessService.callApi(appId, keyName, params);
            ApiResultParser.parser(result);
//            String content = JsonPath.read(result, "$.data.data.audio");
            String content = JsonPath.read(result, "$.data.data");
            byte[] audio = Base64.getDecoder().decode(content);

            return storageService.upload(audio, GlobalAppIdFilter.getAppId(), "mp3", "audio/mpeg");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }
}
