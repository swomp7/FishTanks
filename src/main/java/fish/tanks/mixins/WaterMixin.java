package fish.tanks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fish.tanks.blocks.FishTankBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

// Custom Tank Water Rendering System -- update water only when all other water is updated to avoid lag
@Mixin(FluidRenderer.class)
public abstract class WaterMixin {

    @Invoker("getLight")
    abstract int callGetLight(BlockRenderView world, BlockPos pos);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo cbi) {
        if (blockState.getBlock() instanceof FishTankBlock) {

            Sprite stillSprite = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.WATER.getDefaultState()).particleSprite();
            Sprite overlaySprite = ModelBaker.WATER_OVERLAY.getSprite();

            int color = BiomeColors.getWaterColor(world, pos);
            float red = (float) (color >> 16 & 255) / 255.0F;
            float green = (float) (color >> 8 & 255) / 255.0F;
            float blue = (float) (color & 255) / 255.0F;

            float downBrightness = world.getBrightness(Direction.DOWN, true);
            float upBrightness = world.getBrightness(Direction.UP, true);
            float nsBrightness = world.getBrightness(Direction.NORTH, true);
            float ewBrightness = world.getBrightness(Direction.WEST, true);
            int lightUp = callGetLight(world, pos);
            int lightDown = callGetLight(world, pos.down());
            int lightSide = callGetLight(world, pos);

            float height = world.getBlockState(pos.up()).getBlock() instanceof FishTankBlock ? 1.0F : 0.85F;

            float bx = (float) (pos.getX() & 15);
            float by = (float) (pos.getY() & 15);
            float bz = (float) (pos.getZ() & 15);

            float w = 0.001F;
            
            float minUtb = stillSprite.getMinU();
            float maxUtb = stillSprite.getMaxU();
            float minVtb = stillSprite.getMinV();
            float maxVtb = stillSprite.getMaxV();

            // Top
            if (height == 0.85F || shouldRenderSide(Direction.UP, world, pos)) quad(vertexConsumer, new float[]{bx, bx, bx + 1, bx + 1}, new float[]{by + height, by + height, by + height, by + height}, new float[]{bz, bz + 1, bz + 1, bz}, red * upBrightness, green * upBrightness, blue * upBrightness, new float[]{minUtb, minUtb, maxUtb, maxUtb}, new float[]{maxVtb, minVtb, minVtb, maxVtb}, lightUp);
            if (height == 0.85F || shouldRenderSide(Direction.UP, world, pos)) quad(vertexConsumer, new float[]{bx, bx, bx + 1, bx + 1}, new float[]{by + height, by + height, by + height, by + height}, new float[]{bz + 1, bz, bz, bz + 1}, red * upBrightness, green * upBrightness, blue * upBrightness, new float[]{minUtb, minUtb, maxUtb, maxUtb}, new float[]{maxVtb, minVtb, minVtb, maxVtb}, lightUp);
            
            // Bottom
            if (shouldRenderSide(Direction.DOWN, world, pos)) quad(vertexConsumer, new float[]{bx, bx, bx + 1, bx + 1}, new float[]{by + w, by + w, by + w, by + w}, new float[]{bz + 1, bz, bz, bz + 1}, red * downBrightness, green * downBrightness, blue * downBrightness, new float[]{minUtb, minUtb, maxUtb, maxUtb}, new float[]{maxVtb, minVtb, minVtb, maxVtb}, lightDown);

            // Sides
            for (Direction dir : Direction.Type.HORIZONTAL) {
                Sprite sprite = overlaySprite;

                float r;
                float g;
                float b;
                if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) {
                    r = nsBrightness * red;
                    g = nsBrightness * green;
                    b = nsBrightness * blue;
                } else {
                    r = ewBrightness * red;
                    g = ewBrightness * green;
                    b = ewBrightness * blue;
                }

                float minU = sprite.getFrameU(0.0F);
                float maxU = sprite.getFrameU(0.5F);
                float minV = sprite.getFrameV((1.0F - height) * 0.5F);
                float midV = sprite.getFrameV(0.5F);

                switch (dir) {
                    case NORTH:
                        if (shouldRenderSide(Direction.NORTH, world, pos)) quad(vertexConsumer, new float[]{bx, bx + 1, bx + 1, bx}, new float[]{by + height, by + height, by, by}, new float[]{bz + 0.001F, bz + 0.001F, bz + 0.001F, bz + 0.001F}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{minV, minV, midV, midV}, lightSide);
                        else if (shouldRenderUpper(Direction.NORTH, world, pos)) quad(vertexConsumer, new float[]{bx, bx + 1, bx + 1, bx}, new float[]{by + 1, by + 1, by + 0.85F, by + 0.85F}, new float[]{bz + 0.001F, bz + 0.001F, bz + 0.001F, bz + 0.001F}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{midV, midV, minV, minV}, lightSide);
                        break;
                    case SOUTH:
                        if (shouldRenderSide(Direction.SOUTH, world, pos)) quad(vertexConsumer, new float[]{bx + 1, bx, bx, bx + 1}, new float[]{by + height, by + height, by, by}, new float[]{bz + 0.999F, bz + 0.999F, bz + 0.999F, bz + 0.999F}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{minV, minV, midV, midV}, lightSide);
                        else if (shouldRenderUpper(Direction.SOUTH, world, pos)) quad(vertexConsumer, new float[]{bx + 1, bx, bx, bx + 1}, new float[]{by + 1, by + 1, by + 0.85F, by + 0.85F}, new float[]{bz + 0.999F, bz + 0.999F, bz + 0.999F, bz + 0.999F}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{midV, midV, minV, minV}, lightSide);
                        break;
                    case WEST:
                        if (shouldRenderSide(Direction.WEST, world, pos)) quad(vertexConsumer, new float[]{bx + 0.001F, bx + 0.001F, bx + 0.001F, bx + 0.001F}, new float[]{by + height, by + height, by, by}, new float[]{bz + 1, bz, bz, bz + 1}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{minV, minV, midV, midV}, lightSide);
                        else if (shouldRenderUpper(Direction.WEST, world, pos)) quad(vertexConsumer, new float[]{bx + 0.001F, bx + 0.001F, bx + 0.001F, bx + 0.001F}, new float[]{by + 1, by + 1, by + 0.85F, by + 0.85F}, new float[]{bz + 1, bz, bz, bz + 1}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{midV, midV, minV, minV}, lightSide);
                        break;
                    case EAST:
                        if (shouldRenderSide(Direction.EAST, world, pos)) quad(vertexConsumer, new float[]{bx + 0.999F, bx + 0.999F, bx + 0.999F, bx + 0.999F}, new float[]{by + height, by + height, by, by}, new float[]{bz, bz + 1, bz + 1, bz}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{minV, minV, midV, midV}, lightSide);
                        else if (shouldRenderUpper(Direction.EAST, world, pos)) quad(vertexConsumer, new float[]{bx + 0.999F, bx + 0.999F, bx + 0.999F, bx + 0.999F}, new float[]{by + 1, by + 1, by + 0.85F, by + 0.85F}, new float[]{bz, bz + 1, bz + 1, bz}, r, g, b, new float[]{minU, maxU, maxU, minU}, new float[]{midV, midV, minV, minV}, lightSide);
                        break;
                    default:
                        break;
                }
            }

            cbi.cancel();
        }
    }

    private void quad(VertexConsumer buffer, float[] x, float[] y, float[] z, float r, float g, float b, float[] u, float[] v, int light) {
        for (int i = 0; i < 4; i++) buffer.vertex(x[i], y[i], z[i]).color(r, g, b, 1.0F).texture(u[i], v[i]).light(light).normal(0.0F, 1.0F, 0.0F);
    }

    private boolean shouldRenderSide(Direction dir, BlockRenderView world, BlockPos pos) {
        BlockState block = world.getBlockState(pos.offset(dir));
        if (block.isTransparent() && !(block.getBlock() instanceof FishTankBlock)) return true;
        return false;
    }

    private boolean shouldRenderUpper(Direction dir, BlockRenderView world, BlockPos pos) {
        return world.getBlockState(pos.offset(dir)).getBlock() instanceof FishTankBlock && !(world.getBlockState(pos.offset(dir).up()).getBlock() instanceof FishTankBlock) && world.getBlockState(pos.up()).getBlock() instanceof FishTankBlock;
    }
}
