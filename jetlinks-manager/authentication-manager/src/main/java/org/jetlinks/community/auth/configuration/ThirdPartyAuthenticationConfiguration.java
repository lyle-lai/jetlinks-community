package org.jetlinks.community.auth.configuration;

import org.jetlinks.community.auth.common.AppCredentialsValidator;
import org.jetlinks.community.auth.service.DimensionDeviceService;
import org.jetlinks.community.auth.service.OpenPlatformApiConfigService;
import org.jetlinks.community.auth.service.OpenPlatformAppDeviceAuthService;
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
    public AppCredentialsTokenParser appCredentialsTokenParser(AppCredentialsValidator validator) {
        return new AppCredentialsTokenParser(validator);
    }

    @Bean
    @ConditionalOnBean(OpenPlatformAppService.class)
    public ThirdPartyAppAuthenticationManager thirdPartyAppAuthenticationManager(OpenPlatformAppService appService,
                                                                                 OpenPlatformAppDeviceAuthService deviceAuthService,
                                                                                 DimensionDeviceService dimensionDeviceService) {
        return new ThirdPartyAppAuthenticationManager(appService, deviceAuthService,dimensionDeviceService);
    }

}