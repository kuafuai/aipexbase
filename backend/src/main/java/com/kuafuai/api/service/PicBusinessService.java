package com.kuafuai.api.service;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.parser.ApiResultParser;
import com.kuafuai.common.config.AppConfig;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.file.ImageUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class PicBusinessService {

    @Autowired
    private ApiBusinessService apiBusinessService;

    @Resource
    private StorageService storageService;

    private final static String picDir = AppConfig.getProfile() + "/text2pic";

    public String text2pic(String appId, String text) {
        try {
            String keyName = "word2pic";
            Map<String, Object> params = Maps.newHashMap();
            params.put("text", text);

            String result = apiBusinessService.callApi(appId, keyName, params);
            ApiResultParser.parser(result);
            String content = JsonPath.read(result, "$.data.payload.choices.text[0].content");
            byte[] imageBytes = Base64.getDecoder().decode(content);
            return storageService.upload(imageBytes, GlobalAppIdFilter.getAppId(), "png", "image/png");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    public String pic2word(String appId, String file) {
        try {
            byte[] imageBytes = ImageUtils.readFile(file);
            String base64Content = Base64.getEncoder().encodeToString(imageBytes);

            String keyName = "pic2word";
            Map<String, Object> params = Maps.newHashMap();
            params.put("content", base64Content);

            String result = apiBusinessService.callApi(appId, keyName, params);
            ApiResultParser.parser(result);
            return JsonPath.read(result, "$.data.payload.choices.text[0].content");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }
}
