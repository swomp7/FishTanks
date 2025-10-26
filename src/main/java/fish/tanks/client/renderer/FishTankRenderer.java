package fish.tanks.client.renderer;

import fish.tanks.blocks.FishTankBlockEntity;
import fish.tanks.client.model.FishTankFloorModel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.SeagrassBlock;
import net.minecraft.block.TallSeagrassBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

// Render plants and floor blocks placed in by the player using their models, allowing compatibility with custom blocks as well
public class FishTankRenderer implements BlockEntityRenderer<FishTankBlockEntity> {

    private final FishTankFloorModel floorModel;

    public FishTankRenderer(BlockEntityRendererFactory.Context context) {
        floorModel = new FishTankFloorModel(FishTankFloorModel.getTexturedModelData().createModel());
    }

    @Override
    public void render(FishTankBlockEntity tank, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        if (tank.hasFloor()) {
            matrices.push();
            Block block = Block.getBlockFromItem(tank.getFloor().getItem());
            Identifier floor = Identifier.ofVanilla("textures/block/" + block.toString().replace("Block{minecraft:", "").replace("}", "") + ".png");

            matrices.translate(0.5f, 0.0001f, 0.5f);
            matrices.scale(0.999f, 1.0f, 0.999f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(115.09875F));

            floorModel.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(floor)), light, overlay, cameraPos, tank.getFloorLevel());

            matrices.pop();
        }

        if (tank.hasPlant()) {
            matrices.push();
            Block plant = Block.getBlockFromItem(tank.getPlant().getItem());
            if (plant instanceof KelpBlock && tank.getWorld().getBlockEntity(tank.getPos().up()) instanceof FishTankBlockEntity upTank && upTank.hasPlant()) plant = Blocks.KELP_PLANT;
            BlockState plantState = plant.getDefaultState();
            if (plant instanceof SeagrassBlock && tank.getWorld().getBlockEntity(tank.getPos().up()) instanceof FishTankBlockEntity upTank && upTank.hasPlant()) plantState = Blocks.TALL_SEAGRASS.getDefaultState().with(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER);
            if (plant instanceof SeagrassBlock && tank.getWorld().getBlockEntity(tank.getPos().down()) instanceof FishTankBlockEntity downTank && downTank.hasPlant()) plantState = Blocks.TALL_SEAGRASS.getDefaultState().with(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
            if (plantState.contains(BooleanProperty.of("is3d"))) plantState = plantState.with(BooleanProperty.of("is3d"), false);
            BlockRenderManager manager = MinecraftClient.getInstance().getBlockRenderManager();

            float[] xyz = tank.getPlantPos();
            float floorLevel = tank.getFloorLevel();
            BlockPos downPos = tank.getPos().down();
            while (tank.getWorld().getBlockEntity(downPos) instanceof FishTankBlockEntity downTank) {
                if (downTank.hasFloor()) floorLevel = downTank.getFloorLevel();
                downPos = downPos.down();
            }
            matrices.translate(xyz[0], xyz[1] + ((3f/16f) * floorLevel), xyz[2]);
            matrices.scale(tank.getPlantScale(), tank.getPlantScale(), tank.getPlantScale());

            manager.renderBlockAsEntity(plantState, matrices, vertexConsumers, light, overlay);
            
            matrices.pop();
        }
    }
}
