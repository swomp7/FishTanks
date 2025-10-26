package fish.tanks.items;

import java.util.function.Consumer;

import fish.tanks.registries.FishTankComponents;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TankBuilderItem extends Item {

    private int cooldown = 0;

    public TankBuilderItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (cooldown > 0) {
            cooldown--;
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    // Solve hard to control rotation issue in survival mode
    public boolean canRotate() {
        return cooldown <= 0;
    }

    @Override
    public boolean canMine(ItemStack stack, BlockState state, World world, BlockPos pos, LivingEntity user) {
        return false;
    }

    public void rotate(PlayerEntity player) {
        if (!player.getMainHandStack().contains(FishTankComponents.BUILDER_MODE)) return;
        ItemStack stack = player.getMainHandStack();
        BuilderType[] types = BuilderType.values();
        BuilderType mode = BuilderType.valueOf(stack.get(FishTankComponents.BUILDER_MODE));
        for (int i = 0; i < types.length; i++) if (mode.equals(types[i])) {
            if (i == types.length - 1) mode = types[0];
            else mode = types[i + 1];
            ((ServerPlayerEntity)player).sendMessageToClient(Text.literal(Text.translatable("fish-tanks.tank_builder.set_to").getString() + Text.translatable("fish-tanks.tank_builder." + mode.toString().toLowerCase()).getString()), true);
            if (player.getMainHandStack().getItem() instanceof TankBuilderItem) stack.set(FishTankComponents.BUILDER_MODE, mode.toString());
            break;
        }
        if (!player.isCreative()) cooldown = 2;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        if (stack.contains(FishTankComponents.BUILDER_MODE)) textConsumer.accept(Text.literal(Text.translatable("fish-tanks.tank_builder.current_mode").getString() + Text.translatable("fish-tanks.tank_builder." + stack.get(FishTankComponents.BUILDER_MODE).toLowerCase()).getString()));
    }

    public enum BuilderType {
        REMOVE_FLOOR, REMOVE_PLANT, SET_OPEN_TOP, SET_CONNECTED_TEXTURES, FLOOR_LEVEL;
    }
}
