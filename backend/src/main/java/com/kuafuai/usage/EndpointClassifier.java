package com.kuafuai.usage;

import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * 将原始 (method, path) 归一化为稳定的 endpointGroup 字符串.
 * <p>
 * 有序匹配, 先匹配先赢. 命中的规则决定分组名, 兜底为 "custom" (dynamic API) 或 "_ignore".
 * <p>
 * 分组名故意用业务词汇 (data.invoke / ai.image / auth), 直接展示给用户,
 * 不暴露内部 URL. 想增删接口时改这个文件即可.
 */
public final class EndpointClassifier {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final String IGNORE = "_ignore";

    private static final List<Rule> RULES = Arrays.asList(
            // ---------- 排除, 不入库 ----------
            new Rule(null, "/api/manage/**", IGNORE),  // 后台管理调用
            new Rule(null, "/error/report/**", IGNORE),
            new Rule(null, "/mcp/**", IGNORE),

            // ---------- 数据表 CRUD ----------
            new Rule("POST", "/api/data/invoke", "data.invoke"),
            new Rule("POST", "/api/data/export", "data.export"),
            new Rule("POST", "/api/statistics/invoke", "data.stats"),

            // ---------- AI 系列 ----------
            new Rule("POST", "/api/AiAnalysis", "ai.analysis"),
            new Rule("POST", "/api/elevenLabsTTS", "ai.tts"),
            new Rule("POST", "/api/text2music", "ai.music"),
            new Rule("POST", "/api/word2pic", "ai.image"),
            new Rule("GET", "/api/word2pic/result/*", "ai.image.poll"),

            // ---------- 用户 / 登录 ----------
            new Rule(null, "/login/**", "auth.login"),
            new Rule("POST", "/register", "auth.register"),

            // ---------- 支付 ----------
            new Rule(null, "/generalOrder/**", "order"),

            // ---------- 兜底: 自定义 dynamic API ----------
            new Rule(null, "/api/*", "custom"),
            new Rule(null, "/api/**", "custom")
    );

    private EndpointClassifier() {
    }

    public static String classify(String method, String path) {
        if (path == null) {
            return IGNORE;
        }
        for (Rule r : RULES) {
            if (r.matches(method, path)) {
                return r.group;
            }
        }
        return IGNORE;
    }

    public static boolean isIgnored(String group) {
        return IGNORE.equals(group);
    }

    private static final class Rule {
        final String method;   // null = 任意方法
        final String pattern;
        final String group;

        Rule(String method, String pattern, String group) {
            this.method = method;
            this.pattern = pattern;
            this.group = group;
        }

        boolean matches(String reqMethod, String reqPath) {
            if (method != null && !method.equalsIgnoreCase(reqMethod)) {
                return false;
            }
            return MATCHER.match(pattern, reqPath);
        }
    }
}
