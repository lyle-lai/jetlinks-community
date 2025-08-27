package org.jetlinks.community.auth.dimension;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.OpenPlatformAppEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OpenPlatformAppDimensionProvider extends BaseDimensionProvider<OpenPlatformAppEntity> {

        public OpenPlatformAppDimensionProvider(ReactiveRepository<OpenPlatformAppEntity, String> repository,
                                            ApplicationEventPublisher eventPublisher,
                                            DefaultDimensionUserService dimensionUserService) {
        super(repository, eventPublisher, dimensionUserService);
    }

    @Override
    protected DimensionType getDimensionType() {
        return OpenPlatformDimensionType.openPlatform;
    }

    @Override
    protected Mono<Dimension> convertToDimension(OpenPlatformAppEntity entity) {
        SimpleDimension dimension = new SimpleDimension();
        dimension.setId(entity.getId());
        dimension.setName(entity.getName());
        dimension.setType(getDimensionType());
        return Mono.just(dimension);
    }
}
