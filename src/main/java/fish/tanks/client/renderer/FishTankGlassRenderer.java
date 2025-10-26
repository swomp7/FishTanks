package fish.tanks.client.renderer;

import java.util.HashMap;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlock;
import fish.tanks.blocks.FishTankBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

// Code for customly rendering the glass of a fish tank, allowing for connected textures and algae rendering
public class FishTankGlassRenderer {

    private static final Identifier ALGAE_TEXTURE = Identifier.of(FishTanks.MOD_ID, "block/fish_tank_algae");
    
    @SuppressWarnings("deprecation")
    public static void renderGlass(VertexConsumer vertexConsumer, VertexConsumer algaeVertexConsumer, BlockRenderView world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            float downBrightness = world.getBrightness(Direction.DOWN, true);
            float upBrightness = world.getBrightness(Direction.UP, true);
            float nsBrightness = world.getBrightness(Direction.NORTH, true);
            float ewBrightness = world.getBrightness(Direction.WEST, true);
            int lightUp = getLight(world, pos);
            int lightDown = getLight(world, pos.down());
            int lightSide = getLight(world, pos);

            float bx = (float) (pos.getX() & 15);
            float by = (float) (pos.getY() & 15);
            float bz = (float) (pos.getZ() & 15);

            tank.setShouldUpdateShape(true);

            // Draw in the top glass of the tank if it has an open top rather than a closed one -- depends on surrounding tanks
            if (tank.hasTop() && !tank.hasConnectedTextures()) {
                if (shouldRenderSide(Direction.UP, world, pos)) {
                    HashMap<Identifier, Float[]> textures = tank.makeTopTextureIDs();
                    for (Identifier id : textures.keySet()) {
                        Sprite tex = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(id);
                        float minU = tex.getMinU();
                        float maxU = tex.getMaxU();
                        float minV = tex.getMinV();
                        float maxV = tex.getMaxV();
                        Float[] xys = textures.get(id);
                        quad(vertexConsumer, new float[]{bx + xys[1], bx + xys[0], bx + xys[0], bx + xys[1]}, new float[]{by + 1, by + 1, by + 1, by + 1}, new float[]{bz + 1 - xys[3], bz + 1 - xys[3], bz + 1 - xys[2], bz + 1 - xys[2]}, new float[]{maxU, minU, minU, maxU}, new float[]{minV, minV, maxV, maxV}, lightUp, upBrightness);
                    }
                }
            }

            // If connected textures are enabled, draw in tank glass depending on which tanks are around this one
            if (tank.hasConnectedTextures()) {
                for (Direction dir : Direction.values()) {
                    HashMap<Identifier, Float[]> textures;
                    switch (dir) {
                        case SOUTH: textures = tank.makeTextureIDs(pos.up(), pos.east(), pos.down(), pos.west(), pos.up().east(), pos.down().east(), pos.down().west(), pos.up().west()); break;
                        case EAST: textures = tank.makeTextureIDs(pos.up(), pos.north(), pos.down(), pos.south(), pos.up().north(), pos.down().north(), pos.down().south(), pos.up().south()); break;
                        case WEST: textures = tank.makeTextureIDs(pos.up(), pos.south(), pos.down(), pos.north(), pos.up().south(), pos.down().south(), pos.down().north(), pos.up().north()); break;
                        case UP: textures = tank.hasTop() ? tank.makeTopTextureIDs() : tank.makeTextureIDs(pos.north(), pos.east(), pos.south(), pos.west(), pos.north().east(), pos.south().east(), pos.south().west(), pos.north().west()); break;
                        case DOWN: textures = tank.makeTextureIDs(pos.south(), pos.east(), pos.north(), pos.west(), pos.south().east(), pos.north().east(), pos.north().west(), pos.south().west()); break;
                        // NORTH:
                        default: textures = tank.makeTextureIDs(pos.up(), pos.west(), pos.down(), pos.east(), pos.up().west(), pos.down().west(), pos.down().east(), pos.up().east()); break;
                    }

                    for (Identifier id : textures.keySet()) {
                        Sprite tex = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(id);
                        float minU = tex.getMinU();
                        float maxU = tex.getMaxU();
                        float minV = tex.getMinV();
                        float maxV = tex.getMaxV();
                        Float[] xys = textures.get(id);
                        switch (dir) {
                            case SOUTH: if (shouldRenderSide(Direction.SOUTH, world, pos)) quad(vertexConsumer, new float[]{bx + xys[0], bx + xys[1], bx + xys[1], bx + xys[0]}, new float[]{by + xys[2], by + xys[2], by + xys[3], by + xys[3]}, new float[]{bz + 1, bz + 1, bz + 1, bz + 1}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, nsBrightness); break;
                            case EAST: if (shouldRenderSide(Direction.EAST, world, pos)) quad(vertexConsumer, new float[]{bx + 1, bx + 1, bx + 1, bx + 1}, new float[]{by + xys[2], by + xys[2], by + xys[3], by + xys[3]}, new float[]{bz + 1 - xys[0], bz + 1 - xys[1], bz + 1 - xys[1], bz + 1 - xys[0]}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, ewBrightness); break;
                            case WEST: if (shouldRenderSide(Direction.WEST, world, pos)) quad(vertexConsumer, new float[]{bx, bx, bx, bx}, new float[]{by + xys[2], by + xys[2], by + xys[3], by + xys[3]}, new float[]{bz + xys[0], bz + xys[1], bz + xys[1], bz + xys[0]}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, ewBrightness); break;
                            case UP: if (shouldRenderSide(Direction.UP, world, pos)) quad(vertexConsumer, new float[]{bx + xys[1], bx + xys[0], bx + xys[0], bx + xys[1]}, new float[]{by + 1, by + 1, by + 1, by + 1}, new float[]{bz + 1 - xys[3], bz + 1 - xys[3], bz + 1 - xys[2], bz + 1 - xys[2]}, new float[]{maxU, minU, minU, maxU}, new float[]{minV, minV, maxV, maxV}, lightUp, upBrightness); break;
                            case DOWN: if (shouldRenderSide(Direction.DOWN, world, pos)) quad(vertexConsumer, new float[]{bx + xys[0], bx + xys[1], bx + xys[1], bx + xys[0]}, new float[]{by, by, by, by}, new float[]{bz + xys[2], bz + xys[2], bz + xys[3], bz + xys[3]}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightDown, downBrightness); break;
                            // NORTH: 
                            default: if (shouldRenderSide(Direction.NORTH, world, pos)) quad(vertexConsumer, new float[]{bx + 1 - xys[0], bx + 1 - xys[1], bx + 1 - xys[1], bx + 1 - xys[0]}, new float[]{by + xys[2], by + xys[2], by + xys[3], by + xys[3]}, new float[]{bz, bz, bz, bz}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, nsBrightness); break;
                        }
                    }
                }
            }

            // Draw in algae where needed if dirty
            if (tank.isDirty()) {
                Sprite algae = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(ALGAE_TEXTURE);
                float minU = algae.getMinU();
                float maxU = algae.getMaxU();
                float minV = algae.getMinV();
                float maxV = algae.getMaxV();

                if (shouldRenderSide(Direction.UP, world, pos) && !tank.hasTop()) quad(algaeVertexConsumer, new float[]{bx, bx, bx + 1, bx + 1}, new float[]{by + 1, by + 1, by + 1, by + 1}, new float[]{bz, bz + 1, bz + 1, bz}, new float[]{minU, minU, maxU, maxU}, new float[]{minV, maxV, maxV, minV}, lightUp, upBrightness);
                if (shouldRenderSide(Direction.DOWN, world, pos)) quad(algaeVertexConsumer, new float[]{bx, bx, bx + 1, bx + 1}, new float[]{by, by, by, by}, new float[]{bz + 1, bz, bz, bz + 1}, new float[]{minU, minU, maxU, maxU}, new float[]{minV, maxV, maxV, minV}, lightDown, downBrightness);
                if (shouldRenderSide(Direction.NORTH, world, pos)) quad(algaeVertexConsumer, new float[]{bx + 1, bx, bx, bx + 1}, new float[]{by, by, by + 1, by + 1}, new float[]{bz, bz, bz, bz}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, nsBrightness);
                if (shouldRenderSide(Direction.SOUTH, world, pos)) quad(algaeVertexConsumer, new float[]{bx, bx + 1, bx + 1, bx}, new float[]{by, by, by + 1, by + 1}, new float[]{bz + 1, bz + 1, bz + 1, bz + 1}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, nsBrightness);
                if (shouldRenderSide(Direction.EAST, world, pos)) quad(algaeVertexConsumer, new float[]{bx + 1, bx + 1, bx + 1, bx + 1}, new float[]{by, by, by + 1, by + 1}, new float[]{bz + 1, bz, bz, bz + 1}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, ewBrightness);
                if (shouldRenderSide(Direction.WEST, world, pos)) quad(algaeVertexConsumer, new float[]{bx, bx, bx, bx}, new float[]{by, by, by + 1, by + 1}, new float[]{bz, bz + 1, bz + 1, bz}, new float[]{minU, maxU, maxU, minU}, new float[]{maxV, maxV, minV, minV}, lightSide, ewBrightness);
            }
        }
    }
    
    // Make a quad to draw a texture in
    private static void quad(VertexConsumer buffer, float[] x, float[] y, float[] z, float[] u, float[] v, int light, float brightness) {
        float x0 = x[0], x2 = x[2], y0 = y[0], y2 = y[2], z0 = z[0], z2 = z[2];
        for (int i = 0; i < 4; i++) {
            buffer.vertex(x[i], y[i], z[i]).color(brightness, brightness, brightness, 1.0F).texture(u[i], v[i]).light(light).normal(0.0F, 1.0F, 0.0F);
            x[i] -= x0 == x2 ? 0.002f : 0;
            y[i] -= y0 == y2 ? 0.002f : 0;
            z[i] -= z0 == z2 ? 0.002f : 0;
        }
        for (int i = 3; i >= 0; i--) buffer.vertex(x[i], y[i], z[i]).color(brightness, brightness, brightness, 1.0F).texture(u[i], v[i]).light(light).normal(0.0F, 1.0F, 0.0F);
    }

    private static boolean shouldRenderSide(Direction dir, BlockRenderView world, BlockPos pos) {
        BlockState block;
        switch (dir) {
            case SOUTH: block = world.getBlockState(pos.south()); break;
            case EAST: block = world.getBlockState(pos.east()); break;
            case WEST: block = world.getBlockState(pos.west()); break;
            case UP: block = world.getBlockState(pos.up()); break;
            case DOWN: block = world.getBlockState(pos.down()); break;
            default: block = world.getBlockState(pos.north()); break;
        }
        if (block.isTransparent() && !(block.getBlock() instanceof FishTankBlock)) return true;
        return false;
    }

    private static int getLight(BlockRenderView world, BlockPos pos) {
		int i = WorldRenderer.getLightmapCoordinates(world, pos);
		int j = WorldRenderer.getLightmapCoordinates(world, pos.up());
		int k = i & 255;
		int l = j & 255;
		int m = i >> 16 & 255;
		int n = j >> 16 & 255;
		return (k > l ? k : l) | (m > n ? m : n) << 16;
	}
}
