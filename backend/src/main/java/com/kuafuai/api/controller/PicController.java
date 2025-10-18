package com.kuafuai.api.controller;


import com.kuafuai.api.service.PicBusinessService;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * 图片识别
 */
@RestController
@RequestMapping("/api/pic")
public class PicController {

    @Autowired
    private PicBusinessService picBusinessService;


    @PostMapping("/word2pic")
    public BaseResponse word2Pic(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("text参数不存在，请检查参数！");
        }
        String appId = GlobalAppIdFilter.getAppId();
        String text = String.valueOf(data.get("text"));

        String content = picBusinessService.text2pic(appId, text);
        return ResultUtils.success(content);
    }

    @PostMapping("/pic2word")
    public BaseResponse pic2word(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("file")) {
            return ResultUtils.error("file参数不存在，请检查参数！");
        }
        String appId = GlobalAppIdFilter.getAppId();
        String file = Objects.toString(data.get("file"), "");
        if (StringUtils.isNotEmpty(file)) {
            String content = picBusinessService.pic2word(appId, file);
            return ResultUtils.success(content);
        } else {
            return ResultUtils.error("file 参数为空，请上传file文件");
        }
    }
}
