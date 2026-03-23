package com.kuafuai.accesstoken.strategy;

import com.kuafuai.accesstoken.entity.AppAccessToken;

import javax.servlet.http.HttpServletRequest;

/**
 * 路径鉴权策略接口。
 * 当请求路径命中 allowedPaths 后，若该路径有对应策略，则进入策略进行二次校验。
 */
public interface AccessPathStrategy {

    /**
     * 该策略负责的路径模式列表（AntPath），与 allowedPaths 中的值匹配。
     */


    String pathPattern();

    /**
     * 校验当前请求是否被允许。
     *
     * @param token   当前使用的 AccessToken
     * @param request 完整请求上下文
     * @return true 表示允许，false 表示拒绝
     */
    boolean isAllowed(AppAccessToken token, HttpServletRequest request);
}
