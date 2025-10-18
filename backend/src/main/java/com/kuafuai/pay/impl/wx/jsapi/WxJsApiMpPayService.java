package com.kuafuai.pay.impl.wx.jsapi;


import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 微信公众号的服务
 */
@Component("wx_jsapi_mp")
//@DynamicRefreshScopeAnnotation
@Scope("prototype")
public class WxJsApiMpPayService extends WxJsApiPayService {






    public WxJsApiMpPayService() {
        super();
    }


    public String getAppId() {
        return wxV3PayConfig.getMpAppId();
    }
}
