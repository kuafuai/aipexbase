package com.kuafuai.dynamic.policy;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SQL Token（词法单元）
 */
@Data
@AllArgsConstructor
public class Token {
    /**
     * Token 类型
     */
    private TokenType type;

    /**
     * Token 的原始文本值
     */
    private String value;

    /**
     * Token 在原始 SQL 中的位置（用于错误提示）
     */
    private int position;

    @Override
    public String toString() {
        return String.format("%s(%s)@%d", type, value, position);
    }
}
