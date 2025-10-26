package fish.tanks.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

// Model of a fish tank floor to render
public class FishTankFloorModel {
	private final ModelPart floor, floor2, floor3;

	public FishTankFloorModel(ModelPart root) {
		this.floor = root.getChild("floor");
		this.floor2 = root.getChild("floor2");
		this.floor3 = root.getChild("floor3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData floor = modelPartData.addChild("floor", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -3.0F, -8.0F, 16.0F, 3.0F, 0.0F, new Dilation(0.0F))
		.uv(0, -16).cuboid(-8.0F, -3.0F, -8.0F, 0.0F, 3.0F, 16.0F, new Dilation(0.0F)), ModelTransform.rotation(0.0F, 24.0F, 0.0F));
        floor.addChild("cube_r1", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, 0.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 1.0F, 3.1416F, 0.0F, -3.1416F));
        floor.addChild("cube_r2", ModelPartBuilder.create().uv(0, -16).cuboid(0.0F, -1.5F, -7.0F, 0.0F, 3.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(8.0F, -1.5F, 1.0F, 0.0F, 3.1416F, 0.0F));
        floor.addChild("cube_r3", ModelPartBuilder.create().uv(0, 0).cuboid(-9.0F, -1.5F, 0.0F, 16.0F, 3.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -1.5F, 8.0F, 0.0F, 3.1416F, 0.0F));
        floor.addChild("cube_r4", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, 0.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -3.0F, 1.0F, 0.0F, 3.1416F, 0.0F));

		ModelPartData floor2 = modelPartData.addChild("floor2", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -6.0F, -8.0F, 16.0F, 6.0F, 0.0F, new Dilation(0.0F))
		.uv(0, -16).cuboid(-8.0F, -6.0F, -8.0F, 0.0F, 6.0F, 16.0F, new Dilation(0.0F)), ModelTransform.rotation(0.0F, 24.0F, 0.0F));
        floor2.addChild("cube_r12", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, 0.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 1.0F, 3.1416F, 0.0F, -3.1416F));
        floor2.addChild("cube_r22", ModelPartBuilder.create().uv(0, -16).cuboid(0.0F, -4.5F, -7.0F, 0.0F, 6.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(8.0F, -1.5F, 1.0F, 0.0F, 3.1416F, 0.0F));
        floor2.addChild("cube_r32", ModelPartBuilder.create().uv(0, 0).cuboid(-9.0F, -4.5F, 0.0F, 16.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -1.5F, 8.0F, 0.0F, 3.1416F, 0.0F));
        floor2.addChild("cube_r42", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, -3.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -3.0F, 1.0F, 0.0F, 3.1416F, 0.0F));

		ModelPartData floor3 = modelPartData.addChild("floor3", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -9.0F, -8.0F, 16.0F, 9.0F, 0.0F, new Dilation(0.0F))
		.uv(0, -16).cuboid(-8.0F, -9.0F, -8.0F, 0.0F, 9.0F, 16.0F, new Dilation(0.0F)), ModelTransform.rotation(0.0F, 24.0F, 0.0F));
        floor3.addChild("cube_r13", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, 0.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 1.0F, 3.1416F, 0.0F, -3.1416F));
        floor3.addChild("cube_r23", ModelPartBuilder.create().uv(0, -16).cuboid(0.0F, -7.5F, -7.0F, 0.0F, 9.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(8.0F, -1.5F, 1.0F, 0.0F, 3.1416F, 0.0F));
        floor3.addChild("cube_r33", ModelPartBuilder.create().uv(0, 0).cuboid(-9.0F, -7.5F, 0.0F, 16.0F, 9.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -1.5F, 8.0F, 0.0F, 3.1416F, 0.0F));
        floor3.addChild("cube_r43", ModelPartBuilder.create().uv(-16, 0).cuboid(-9.0F, -6.0F, -7.0F, 16.0F, 0.0F, 16.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -3.0F, 1.0F, 0.0F, 3.1416F, 0.0F));

		return TexturedModelData.of(modelData, 16, 16);
	}

	public void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay, Vec3d vec, int floorLevel) {
		if (floorLevel == 1) {
			floor.render(matrices, buffer, light, overlay);
		} else if (floorLevel == 2) {
			floor2.render(matrices, buffer, light, overlay);
		} else {
			floor3.render(matrices, buffer, light, overlay);
		}
	}
}
