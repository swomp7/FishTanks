package fish.tanks.client;

import fish.tanks.client.renderer.FishTankRenderer;
import fish.tanks.registries.FishTankBlocks;
import fish.tanks.registries.FishTankPackets;
import fish.tanks.registries.FishTankScreens;
import fish.tanks.screens.FilterScreen;
import fish.tanks.screens.FishTankScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

@Environment(EnvType.CLIENT)
public class FishTanksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(FishTankBlocks.FISH_TANK, BlockRenderLayer.CUTOUT);
        HandledScreens.register(FishTankScreens.FISH_TANK_SCREEN_HANDLER, FishTankScreen::new);
        HandledScreens.register(FishTankScreens.FILTER_SCREEN_HANDLER, FilterScreen::new);

        BlockEntityRendererFactories.register(FishTankBlocks.FISH_TANK_BE, FishTankRenderer::new);
        FishTankPackets.registerClient();
    }
}
