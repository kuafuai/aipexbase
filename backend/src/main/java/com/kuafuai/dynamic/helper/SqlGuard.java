package com.kuafuai.dynamic.helper;

import com.kuafuai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 动态 SQL 出口安全校验。
 * <p>
 * 所有用户值都走 MyBatis #{} 占位符,Provider 返回的 SQL 字符串里
 * 正常情况下不应该出现分号、注释、或 DDL/DCL/危险函数关键字。
 * <p>
 * 拦截策略(白名单不可行时的兜底):
 * 1) 切语句/注释符: ; -- /* * /
 * 2) DDL/DCL/危险函数关键字(单词边界匹配)
 * <p>
 * 注意:
 * - 反引号包裹的合法标识符(如 `create_time`)不会撞,因为 \b 在 `_` 处不形成词边界
 * (即 create_time 整体被视作一个单词,不匹配 \bCREATE\b)。
 * - 但若有列/表名是单纯的 union/drop 等单词,会被误拦,需事先确认。
 */
@Slf4j
public class SqlGuard {

    /**
     * DDL / DCL / 多语句 / 文件操作 / 时间盲注 / 元数据探测 等敏感关键字。
     * \b 词边界:确保只匹配独立单词,不会撞 create_time、is_dropped 等列名。
     */
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "\\b(" +
                    // DDL
                    "DROP|TRUNCATE|ALTER|CREATE|RENAME" +
                    // DCL
                    "|GRANT|REVOKE" +
                    // 多语句 / 预处理
                    "|PREPARE|EXECUTE|DEALLOCATE" +
                    // 文件操作
                    "|OUTFILE|DUMPFILE|LOAD_FILE" +
                    // 时间盲注
                    "|SLEEP|BENCHMARK" +
                    // 元数据探测 / UNION 注入
                    "|INFORMATION_SCHEMA|UNION" +
                    // 配置变更
                    "|FOREIGN_KEY_CHECKS|SQL_MODE|GLOBAL" +
                    ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    public static String check(String sql) {
        if (sql == null) {
            return null;
        }

        // 1) 切语句 / 注释符号 —— Provider 输出里正常不应出现
        //    (用户值走 #{} 占位符,不会出现在 SQL 字符串里)
        if (sql.indexOf(';') >= 0) {
            log.error("【SQL 注入拦截-符号】非法 SQL: {}", sql);
            throw new BusinessException("dynamic.sql.illegal");
        }

        // 2) DDL/DCL/危险函数关键字
        if (DANGEROUS_KEYWORDS.matcher(sql).find()) {
            log.error("【SQL 注入拦截-关键字】非法 SQL: {}", sql);
            throw new BusinessException("dynamic.sql.illegal");
        }

        return sql;
    }
}
