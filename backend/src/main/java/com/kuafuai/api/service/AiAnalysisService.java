package com.kuafuai.api.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * AiAnalysis 图文理解服务（去 Dify 版）。
 *
 * 对接标准 OpenAI 兼容聊天补全端点（api.openai-next.com /v1/chat/completions）：
 *  - 入参 prompt(职责) / query(用户需求) / files(图片) 组装成 messages。
 *  - 复用 ApiBusinessService 的计费+调用链路（api_market id=18 配置指向网关）。
 *  - 把 OpenAI 响应回填成 Dify 结构 {answer, conversation_id, metadata.usage}，
 *    保持前端契约不变（前端仍读 .answer）。
 */
@Service
@Slf4j
public class AiAnalysisService {

    public static final String API_KEY = "AiAnalysis";
    /** 默认模型名，可被入参 model 覆盖； */
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    @Autowired
    private ApiBusinessService apiBusinessService;

    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Object>>() {
    }.getType();

    public Object analyze(String appId, Map<String, Object> data) {
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, API_KEY);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "API配置不存在: " + API_KEY);
        }

        // 1) 组装 OpenAI messages（关键转换：files -> content[].image_url）
        List<Object> messages = buildMessages(data);

        // 2) 组装模板参数：${{model}} 与 ${{messages}} 注入 body_template
        Map<String, Object> params = Maps.newHashMap();
        params.put("model", data.getOrDefault("model", DEFAULT_MODEL));
        // messages 已是 JSON 数组字符串，ApiClient 遇到 "[" 开头会原样注入（不转义）
        params.put("messages", gson.toJson(messages));
        // 透传环境变量（与通用链路一致）
        if (data.containsKey("ip")) {
            params.put("ip", data.get("ip"));
        }

        // 3) 走计费+调用；data_path=$ 拿整包 OpenAI 响应
        String raw;
        if (setting.getMarketId() != null && setting.getMarketId() > 0) {
            raw = apiBusinessService.callApiWithBilling(appId, setting, params);
        } else {
            raw = apiBusinessService.callHttpApi(setting, params, null);
        }

        // 4) OpenAI -> Dify 形状回填，保持前端契约
        return ResultUtils.success(toDifyShape(raw, data));
    }

    /**
     * 构建 messages：
     *  system: 复刻 Dify 提示词（职责 + 用户需求 + 结论要求）
     *  user  : [ {text: query}, {image_url:{url}} ... ]
     */
    private List<Object> buildMessages(Map<String, Object> data) {
        String prompt = str(data.get("prompt"));
        String query = str(data.get("query"));

        List<Object> messages = Lists.newArrayList();

        // system —— 与原 Dify workflow 的 system prompt 等价
        String systemText = "您的职责：" + prompt
                + "\n用户的需求：" + query
                + "\n请你根据你的职责，结合用户需求给出最优的结论";
        messages.add(textMessage("system", systemText));

        // user content 数组：先文本，再图片
        List<Object> userContent = Lists.newArrayList();
        Map<String, Object> textPart = Maps.newHashMap();
        textPart.put("type", "text");
        textPart.put("text", StringUtils.isNotEmpty(query) ? query : "请根据图片给出分析结论");
        userContent.add(textPart);

        for (String url : extractImageUrls(data)) {
            Map<String, Object> imgUrl = Maps.newHashMap();
            imgUrl.put("url", url);
            Map<String, Object> imgPart = Maps.newHashMap();
            imgPart.put("type", "image_url");
            imgPart.put("image_url", imgUrl);
            userContent.add(imgPart);
        }

        Map<String, Object> userMsg = Maps.newHashMap();
        userMsg.put("role", "user");
        userMsg.put("content", userContent);
        messages.add(userMsg);

        return messages;
    }

    private Map<String, Object> textMessage(String role, String text) {
        Map<String, Object> msg = Maps.newHashMap();
        msg.put("role", role);
        msg.put("content", text);
        return msg;
    }

    /**
     * 兼容多种 files 入参：
     *  - 字符串单个 url
     *  - ["url", ...] 字符串数组
     *  - [{"url": "..."}] 或 Dify 风格 [{"url":"...","type":"image"}] 对象数组
     *  - 额外兼容单字段 file
     */
    @SuppressWarnings("unchecked")
    private List<String> extractImageUrls(Map<String, Object> data) {
        List<String> urls = Lists.newArrayList();
        addUrl(urls, data.get("file"));

        Object files = data.get("files");
        if (files instanceof String) {
            String s = ((String) files).trim();
            // files 可能被前端序列化成 JSON 字符串
            if (s.startsWith("[")) {
                try {
                    List<Object> arr = gson.fromJson(s, listType);
                    for (Object o : arr) {
                        addUrl(urls, o);
                    }
                } catch (Exception ignore) {
                    addUrl(urls, s);
                }
            } else if (StringUtils.isNotEmpty(s)) {
                addUrl(urls, s);
            }
        } else if (files instanceof List) {
            for (Object o : (List<Object>) files) {
                addUrl(urls, o);
            }
        }
        return urls;
    }

    @SuppressWarnings("unchecked")
    private void addUrl(List<String> urls, Object item) {
        if (item == null) {
            return;
        }
        if (item instanceof String) {
            String s = (String) item;
            if (StringUtils.isNotEmpty(s)) {
                urls.add(s);
            }
        } else if (item instanceof Map) {
            Object url = ((Map<String, Object>) item).get("url");
            if (url instanceof String && StringUtils.isNotEmpty((String) url)) {
                urls.add((String) url);
            }
        }
    }

    /**
     * OpenAI 响应 -> Dify 结构。
     * 提取 choices[0].message.content 作为 answer；usage.total_tokens 放进 metadata（计费/前端可读）。
     * 额外回填 message_id(<-id) / created_at(<-created)，兼容读取这些字段的老应用。
     */
    private Map<String, Object> toDifyShape(String raw, Map<String, Object> data) {
        String answer;
        try {
            answer = JsonPath.read(raw, "$.choices[0].message.content");
        } catch (Exception e) {
            // 详细原因（JsonPath 报错 + 上游原始响应）只进日志，不外泄给用户
            log.error("=====AiAnalysis 解析响应失败:{}=====\n{}", e.getMessage(), raw);
            throw new BusinessException("AI分析失败，请稍后重试");
        }
        if (StringUtils.isEmpty(answer)) {
            log.error("=====AiAnalysis 响应内容为空=====\n{}", raw);
            throw new BusinessException("AI分析失败，请稍后重试");
        }

        Integer totalTokens = null;
        try {
            Object t = JsonPath.read(raw, "$.usage.total_tokens");
            if (t instanceof Number) {
                totalTokens = ((Number) t).intValue();
            }
        } catch (Exception ignore) {
            // usage 缺失不影响主流程
        }

        Map<String, Object> usage = Maps.newHashMap();
        if (totalTokens != null) {
            usage.put("total_tokens", totalTokens);
        }
        Map<String, Object> metadata = Maps.newHashMap();
        metadata.put("usage", usage);

        Map<String, Object> out = Maps.newHashMap();
        out.put("answer", answer);
        out.put("conversation_id", str(data.get("conversation_id")));
        out.put("metadata", metadata);
        // 兼容老应用：message_id <- id，created_at <- created
        out.put("message_id", readQuietly(raw, "$.id"));
        out.put("created_at", readQuietly(raw, "$.created"));
        return out;
    }

    /** 安全读取 JsonPath，缺失/异常返回 null，不打断主流程。 */
    private Object readQuietly(String raw, String path) {
        try {
            return JsonPath.read(raw, path);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
