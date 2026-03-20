package com.kuafuai.manage.config;

import com.kuafuai.manage.filter.ManageApiAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 管理API安全配置
 * 用于对外开放的管理接口（创建应用、创建表等）
 */
@Slf4j
@Configuration
@Order(2)
public class ManageApiSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/api/manage/**")
                .cors().and()
                .csrf().disable()
                .headers().cacheControl().disable().and()
                .headers().frameOptions().disable().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .addFilterBefore(new ManageApiAuthFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
