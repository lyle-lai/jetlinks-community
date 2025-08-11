package org.jetlinks.community.device.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceMetadataMappingDetail;
import org.jetlinks.community.device.entity.DeviceMetadataMappingEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class DeviceMetadataMappingService extends GenericReactiveCrudService<DeviceMetadataMappingEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService productService;

    private final LocalDeviceInstanceService deviceService;

    private final LoadingCache<String, List<DeviceMetadataMappingDetail>> deviceMappingCache = Caffeine.newBuilder()
                                                                                                       .expireAfterWrite(5, TimeUnit.MINUTES)
                                                                                                       .maximumSize(10_000)
                                                                                                       .build(this::loadDeviceMappingDetails);

    private List<DeviceMetadataMappingDetail> loadDeviceMappingDetails(String deviceId) {
        return getDeviceMappingDetail(deviceId)
            .collectList()
            .block(); // 使用block是因为Caffeine加载器需要同步返回
    }

    //因为增删改的实体中不一定有设备id,所以此处使所有缓存都失效重新构建

    @EventListener
    public void handleEvent(EntitySavedEvent<DeviceMetadata> event) {
        deviceMappingCache.invalidateAll();
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<DeviceMetadata> event) {
        deviceMappingCache.invalidateAll();
    }
    @EventListener
    public void handleEvent(EntityCreatedEvent<DeviceMetadata> event) {
        deviceMappingCache.invalidateAll();
    }
    @EventListener
    public void handleEvent(EntityDeletedEvent<DeviceMetadata> event) {
        deviceMappingCache.invalidateAll();
    }



    public Flux<DeviceMetadataMappingDetail> getProductMappingDetail(String productId) {
        return this
            .getProductMetadata(productId)
            .flatMapMany(metadata -> this
                .convertDetail(metadata,
                               this
                                   .createQuery()
                                   .where(DeviceMetadataMappingEntity::getProductId, productId)
                                   .isNull(DeviceMetadataMappingEntity::getDeviceId)
                                   .fetch(),
                               () -> DeviceMetadataMappingDetail.ofProduct(productId)
                ));
    }

    public Flux<DeviceMetadataMappingDetail> getDeviceMappingDetail(String deviceId) {
        return deviceService
            .findById(deviceId)
            .flatMapMany(device -> this
                .getDeviceMetadata(device)
                .flatMapMany(metadata -> this
                    .convertDetail(
                        metadata,
                        this
                            .createQuery()
                            //where product_id =? and (device_id is null or device_id = ?)
                            .where(DeviceMetadataMappingEntity::getProductId, device.getProductId())
                            .nest()
                            .isNull(DeviceMetadataMappingEntity::getDeviceId)
                            .or()
                            .is(DeviceMetadataMappingEntity::getDeviceId, deviceId)
                            .end()
                            .fetch(),
                        () -> DeviceMetadataMappingDetail.ofDevice(device.getProductId(), deviceId))
                ));
    }

    public Mono<Void> saveDeviceMapping(String deviceId, Flux<DeviceMetadataMappingEntity> mappings) {

       return mappings
            .groupBy(e -> StringUtils.hasText(e.getOriginalId()))
            .flatMap(group -> {
                //bind
                if (group.key()) {
                    return deviceService
                        .findById(deviceId)
                        .flatMap(device -> this.save(
                            group.doOnNext(e -> {
                                e.setDeviceId(deviceId);
                                e.setProductId(device.getProductId());
                                e.generateId();
                                e.tryValidate(CreateGroup.class);
                            })
                        ));
                }
                //unbind
                return group
                    .map(mapping -> DeviceMetadataMappingEntity
                        .generateIdByDevice(deviceId, mapping.getMetadataType(), mapping.getMetadataId()))
                    .as(this::deleteById)
                    .then();
            })
           .then();
    }

    public Mono<Void> saveProductMapping(String productId, Flux<DeviceMetadataMappingEntity> mappings) {
        return mappings
            .groupBy(e -> StringUtils.hasText(e.getOriginalId()))
            .flatMap(group -> {
                //bind
                if (group.key()) {
                    return productService
                        .findById(productId)
                        .flatMap(device -> this.save(
                            group.doOnNext(e -> {
                                e.setDeviceId(null);
                                e.setProductId(productId);
                                e.generateId();
                                e.tryValidate(CreateGroup.class);
                            })
                        ));
                }
                //unbind
                return group
                    .map(mapping -> DeviceMetadataMappingEntity
                        .generateIdByProduct(productId, mapping.getMetadataType(), mapping.getMetadataId()))
                    .as(this::deleteById)
                    .then();
            })
            .then();
    }

    private Mono<DeviceMetadata> getProductMetadata(String productId) {
        //从数据库中获取物模型?
        return productService
            .findById(productId)
            .flatMap(product -> JetLinksDeviceMetadataCodec.getInstance().decode(product.getMetadata()));
    }

    private Mono<DeviceMetadata> getDeviceMetadata(DeviceInstanceEntity device) {
        if (StringUtils.hasText(device.getDeriveMetadata())) {
            return JetLinksDeviceMetadataCodec.getInstance().decode(device.getDeriveMetadata());
        }
        return getProductMetadata(device.getProductId());
    }

    private Flux<DeviceMetadataMappingDetail> convertDetail(ThingMetadata metadata,
                                                            Flux<DeviceMetadataMappingEntity> mappings,
                                                            Supplier<DeviceMetadataMappingDetail> builder) {

        return mappings
            .collect(Collectors.toMap(DeviceMetadataMappingEntity::getMetadataId,
                                      Function.identity(),
                                      //有设备ID则以设备配置的为准
                                      (left, right) -> StringUtils.hasText(left.getDeviceId()) ? left : right))
            .flatMapMany(mapping -> Flux
                .fromIterable(metadata.getProperties())
                .map(property -> builder
                    .get()
                    .with(property)
                    .with(mapping.get(property.getId()))));
    }

    public Mono<DeviceMessage> transformDeviceData(DeviceMessage message) {
        if (message.getMessageType() == MessageType.REPORT_PROPERTY) {
            ReportPropertyMessage reportMessage = (ReportPropertyMessage) message;
            return transformDeviceProperties(message.getDeviceId(), reportMessage.getProperties())
                .doOnNext(reportMessage::setProperties)
                .thenReturn(message);
        } else if (message.getMessageType() == MessageType.READ_PROPERTY_REPLY) {
            ReadPropertyMessageReply replyMessage = (ReadPropertyMessageReply) message;
            return transformDeviceProperties(message.getDeviceId(), replyMessage.getProperties())
                .doOnNext(replyMessage::setProperties)
                .thenReturn(message);
        }
        return Mono.just(message);
    }

    public Mono<Map<String, Object>> transformDeviceProperties(String deviceId, Map<String, Object> rawData) {
        return Mono.fromSupplier(() -> deviceMappingCache.get(deviceId))
                   .subscribeOn(Schedulers.boundedElastic())
                   .map(mappings -> applyMappings(rawData, mappings))
                   .onErrorResume(e -> {
                       log.error("转换设备数据失败: {}", e.getMessage(), e);
                       return Mono.just(rawData); // 失败时返回原始数据
                   });
    }

    private Map<String, Object> applyMappings(Map<String, Object> rawData, List<DeviceMetadataMappingDetail> mappings) {
        Map<String, DeviceMetadataMappingDetail> mappingCache = mappings.stream()
                                                                        .filter(DeviceMetadataMappingDetail::isCustomMapping)
                                                                        .collect(Collectors.toMap(
                                                                            DeviceMetadataMappingDetail::getMetadataId,
                                                                            mapping -> mapping
                                                                        ));
        // 循环mappingCache,因为可能存在多个物模型id对应一个原始id
        Map<String, Object> transformedData = new HashMap<>(rawData.size());
        for (Map.Entry<String, DeviceMetadataMappingDetail> entry : mappingCache.entrySet()) {
            String metadataId = entry.getKey();
            DeviceMetadataMappingDetail mapping = entry.getValue();
            String originalProperty = mapping.getOriginalId();
            Object originalValue = rawData.get(originalProperty);
            if (originalValue != null) {
                transformedData.put(metadataId, applyValueTransformation(originalValue, mapping.getOthers()));
            }
            else {
                transformedData.put(metadataId, originalValue);
            }
        }

        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String originalProperty = entry.getKey();
            Object originalValue = entry.getValue();

            DeviceMetadataMappingDetail mapping = mappingCache.get(originalProperty);

            if (mapping != null) {
                if (mapping.getOthers() == null || mapping.getOthers().isEmpty() || !mapping.getOthers().containsKey("transformationConfig")) {
                    transformedData.put(originalProperty, originalValue);
                }
                else {
                    Map<String, Object> transformationConfig = (Map<String, Object>) mapping.getOthers().get("transformationConfig");
                    transformedData.put(mapping.getMetadataId(), applyValueTransformation(originalValue, transformationConfig));
                }
            } else {
                transformedData.put(originalProperty, originalValue);
            }
        }
        return transformedData;
    }

    /**
     * 应用值转换逻辑,规则如下:
     * 1. 枚举映射: {"transformationConfig":{"type":"mapping","rules":[{"source":"1s","target":"d"},{"source":"4","target":"5"},{"source":"a","target":"b"}]}}
     * 2. 数学计算: {"transformationConfig":{"type":"math","expression":"(x - 32) * 5/9"}}
     * @param originalValue 原始值
     * @param transformationRules 转换规则 (从映射配置的others中获取)
     * @return 转换后的值
     */
    private Object applyValueTransformation(Object originalValue, Map<String, Object> transformationRules) {
        if (transformationRules == null || transformationRules.isEmpty() || !transformationRules.containsKey("transformationConfig")) {
            return originalValue;
        }
        Map<String, Object> transformationConfig = (Map<String, Object>) transformationRules.get("transformationConfig");

        // 枚举映射
        if (transformationConfig.containsKey("type") && "mapping".equals(transformationConfig.get("type"))) {
            Map<String, Object> enumMap = (Map<String, Object>) transformationConfig.get("rules");
            return enumMap.getOrDefault(originalValue.toString(), originalValue);
        }
        // 数学计算
        if (transformationConfig.containsKey("type") && "math".equals(transformationConfig.get("type"))) {
            String formula = (String) transformationConfig.get("expression");
            return calculateFormula(originalValue, formula);
        }

        return originalValue;
    }

    private Object calculateFormula(Object originalValue, String formula) {
        Expression expression = new ExpressionBuilder(formula)
            .variable("x")
            .build()
            .setVariable("x", Double.parseDouble(originalValue.toString()));

        return expression.evaluate();
    }


}
