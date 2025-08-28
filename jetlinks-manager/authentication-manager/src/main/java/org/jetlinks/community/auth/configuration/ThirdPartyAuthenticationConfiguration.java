package org.jetlinks.community.auth.configuration;

import org.jetlinks.community.auth.service.OpenPlatformApiConfigService;
import org.jetlinks.community.auth.service.OpenPlatformAppService;
import org.jetlinks.community.auth.thirdpart.AppCredentialsTokenParser;
import org.jetlinks.community.auth.thirdpart.ThirdPartyAppAuthenticationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisOperations;

@Configuration(proxyBeanMethods = false)
public class ThirdPartyAuthenticationConfiguration {

    @Bean
    @ConditionalOnBean(OpenPlatformAppService.class)
    public AppCredentialsTokenParser appCredentialsTokenParser(OpenPlatformAppService appService,
                                                             OpenPlatformApiConfigService apiConfigService,
                                                             ReactiveRedisOperations<String, String> redis) {
        return new AppCredentialsTokenParser(appService, apiConfigService, redis);
    }

    @Bean
    @ConditionalOnBean(OpenPlatformAppService.class)
    public ThirdPartyAppAuthenticationManager thirdPartyAppAuthenticationManager(OpenPlatformAppService appService) {
        return new ThirdPartyAppAuthenticationManager(appService);
    }

}