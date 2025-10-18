package com.kuafuai.login.service;

import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.constant.CacheConstants;
import com.kuafuai.common.constant.Constants;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.IdUtils;
import com.kuafuai.common.util.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TokenService {
    @Value("${token.header:Authorization}")
    private String header;

    @Value("${token.secret:abcdefghijklmnopqrstuvwxyz}")
    private String secret;

    @Value("${token.expireTime:30}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;
    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;
    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;

    @Autowired
    private Cache cache;

    public LoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            try {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                return cache.getCacheObject(userKey);
            } catch (Exception e) {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }


    public String createToken(LoginUser loginUser) {
        String token = IdUtils.fastUUID();
        log.info("============user login token : {}", token);
        loginUser.setToken(token);

        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        return createToken(claims);
    }

    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userKey = getTokenKey(token);
            cache.deleteObject(userKey);
        }
    }

    public boolean verifyToken(LoginUser loginUser, String appId) {
        if (StringUtils.equalsIgnoreCase(appId, loginUser.getAppId())) {
            long expireTime = loginUser.getExpireTime();
            long currentTime = System.currentTimeMillis();
            if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
                refreshToken(loginUser);
            }
            return true;
        } else {
            return false;
        }
    }

    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(LoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);

        String userKey = getTokenKey(loginUser.getToken());
        cache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }


    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

}
