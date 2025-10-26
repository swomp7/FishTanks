package fish.tanks.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.VertexSorter;

import fish.tanks.blocks.FishTankBlock;
import fish.tanks.client.renderer.FishTankGlassRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.chunk.SectionBuilder.RenderData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

// Calls custom glass and algae rendering to allow a dirty tank view and connected textures
@Mixin(SectionBuilder.class)
public abstract class RenderGlassMixin {

    @Invoker("beginBufferBuilding")
    abstract BufferBuilder callBeginBufferBuilding(Map<BlockRenderLayer, BufferBuilder> builders, BlockBufferAllocatorStorage allocatorStorage, BlockRenderLayer layer);

    // Called only when all other block textures are updated to avoid lag
    @Inject(method = "build", at = @At(value = "INVOKE", target = "net/minecraft/block/BlockState.getRenderType()Lnet/minecraft/block/BlockRenderType;", shift = Shift.BEFORE))
	private void renderGlass(ChunkSectionPos sectionPos, ChunkRendererRegion renderRegion, VertexSorter vertexSorter, BlockBufferAllocatorStorage allocatorStorage, CallbackInfoReturnable<RenderData> cir, @Local(ordinal = 0) Map<BlockRenderLayer, BufferBuilder> map, @Local(ordinal = 2) BlockPos pos, @Local(ordinal = 0) BlockState state) {
        if (state.getBlock() instanceof FishTankBlock) {
            BufferBuilder bufferBuilder = callBeginBufferBuilding(map, allocatorStorage, state.get(FishTankBlock.IS_DIRTY) ? BlockRenderLayer.TRIPWIRE : RenderLayers.getBlockLayer(state));
            BufferBuilder algaeBufferBuilder = callBeginBufferBuilding(map, allocatorStorage, BlockRenderLayer.TRIPWIRE);
            FishTankGlassRenderer.renderGlass(bufferBuilder, algaeBufferBuilder, renderRegion, pos);
        }
    }
}
