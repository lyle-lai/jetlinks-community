package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;

public class HotColdTDengineStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TDengineThingDataHelper helper;

    public HotColdTDengineStrategy(ThingsRegistry registry, TDengineThingDataHelper helper) {
        this.registry = registry;
        this.helper = helper;
    }

    @Override
    public String getId() {
        return "tdengine-hot-cold";
    }

    @Override
    public String getName() {
        return "TDengine-列式-高低频分离存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new HotColdSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            helper);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new HotColdDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            helper);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new HotColdQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            helper);
    }

    @Override
    public int getOrder() {
        return 10230; // 优先级略低于其他tdengine策略
    }
}
