package com.coolfreeze23.vbparity.variants.client;

import com.coolfreeze23.vbparity.variants.client.model.BabyCatModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyChickenModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyCowModel;
import com.coolfreeze23.vbparity.variants.client.model.BabyPigModel;
import com.coolfreeze23.vbparity.variants.client.model.BabySheepModels;
import com.coolfreeze23.vbparity.variants.client.model.BabyWolfModel;
import com.coolfreeze23.vbparity.variants.client.model.TemperateCowModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "vbparity", value = Dist.CLIENT)
public final class VariantsClient {
    private VariantsClient() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BabyRendering.TEMPERATE_COW, TemperateCowModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.COW_BABY, BabyCowModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.PIG_BABY, BabyPigModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.CHICKEN_BABY, BabyChickenModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.SHEEP_BABY, BabySheepModels::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.SHEEP_BABY_WOOL, BabySheepModels::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.WOLF_BABY, BabyWolfModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.CAT_BABY, BabyCatModel::createBodyLayer);
        event.registerLayerDefinition(BabyRendering.CAT_BABY_COLLAR, BabyCatModel::createBodyLayer);
    }
}
