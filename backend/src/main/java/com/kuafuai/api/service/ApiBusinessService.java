package com.kuafuai.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.client.ApiClient;
import com.kuafuai.api.parser.ApiResultParser;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.IdUtils;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.DynamicApiSettingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class ApiBusinessService {


    @Autowired
    private DynamicApiSettingService dynamicApiSettingService;


    private final ApiClient apiClient = new ApiClient();

    public String callApi(String appId, String apiKey, Map<String, Object> params) {
        // 查询 api 记录
        DynamicApiSetting setting = getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (StringUtils.equalsIgnoreCase(setting.getProtocol(), "http")) {
            return callHttpApi(setting, params);
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    public String callHttpApi(DynamicApiSetting setting, Map<String, Object> params) {

        ApiDefinition apiDefinition = getApiBySetting(setting);
        Map<String, String> fixedParams = Maps.newHashMap();
        // 判断鉴权方式
        if (StringUtils.isNotEmpty(setting.getToken())) {
            fixedParams.put("token", setting.getToken());
        }

        // 合并 params
        params.putAll(fixedParams);

        return apiClient.call(apiDefinition, params);
    }

    public DynamicApiSetting getByApiKey(String appId, String apiKey) {
        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);
        queryWrapper.eq(DynamicApiSetting::getKeyName, apiKey);

        return dynamicApiSettingService.getOne(queryWrapper);
    }

    private ApiDefinition getApiBySetting(DynamicApiSetting setting) {
        return ApiDefinition.builder()
                .name(setting.getKeyName())
                .method(setting.getMethod())
                .url(setting.getUrl())
                .headers(setting.getHeader())
                .bodyType(setting.getBodyType())
                .bodyTemplate(setting.getBodyTemplate())
                .build();
    }


    /**
     * 识别图片中的内容
     *
     * @param image
     * @param format
     * @return
     */
    public String pic2word(String appId, BufferedImage image, String format) {
        try {
            byte[] imageBytes = imageToByte(image, format);
            String base64Content = Base64.getEncoder().encodeToString(imageBytes);

            String keyName = "pic2word";
            Map<String, Object> params = Maps.newHashMap();
            params.put("content", base64Content);

            String result = callApi(appId, keyName, params);
            ApiResultParser.parser(result);
            return JsonPath.read(result, "$.data.payload.choices.text[0].content");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 文本2文本
     *
     * @param text
     * @param prompt
     * @return
     */
    public String text2text(String appId, String text, String prompt) {
        try {
            String keyName = "text2text";
            Map<String, Object> params = Maps.newHashMap();
            params.put("query", text);
            Map<String, Object> inputs = Maps.newHashMap();
            inputs.put("prompt", com.kuafuai.common.util.StringUtils.isNotEmpty(prompt) ? prompt : "用户与你的对话只会进行一次,之后也不会和你再次进行对话,你需要尽可能的使用用户提供的信息,完成用户的需求,并且使用合理的语言表述出来,step by step");
            params.put("inputs", inputs);
            params.put("response_mode", "blocking");
            params.put("conversation_id", "");
            params.put("user", IdUtils.simpleUUID());

            String result = callApi(appId, keyName, params);
            log.info("text2text result: {}", result);
            ApiResultParser.parser(result);
            return JsonPath.read(result, "$.data.answer");

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private byte[] imageToByte(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}
