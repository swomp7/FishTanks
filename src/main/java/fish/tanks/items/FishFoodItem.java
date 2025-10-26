package fish.tanks.items;

import java.util.function.Consumer;

import fish.tanks.registries.FishTankComponents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public class FishFoodItem extends Item {

    public FishFoodItem(Settings settings) {
        super(settings);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        if (stack.contains(FishTankComponents.FOOD_STATUS)) {
            Integer foodStatus = stack.get(FishTankComponents.FOOD_STATUS);
            if (foodStatus != null) {
                textConsumer.accept(Text.literal(Text.translatable("fish-tanks.fish_food.food_status").getString() + foodStatus + " / " + 64));
            }
        }
    }
}
