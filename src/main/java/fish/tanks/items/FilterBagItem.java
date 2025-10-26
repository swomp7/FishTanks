package fish.tanks.items;

import java.util.function.Consumer;

import fish.tanks.registries.FishTankComponents;
import fish.tanks.tank.FilterDataStorage;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public class FilterBagItem extends Item {

    public FilterBagItem(Settings settings) {
        super(settings);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        if (stack.contains(FishTankComponents.FILTER_DATA)) {
            FilterDataStorage data = stack.get(FishTankComponents.FILTER_DATA);
            if (data != null) {
                textConsumer.accept(Text.literal(Text.translatable("fish-tanks.filter_bag.filter_status").getString() + ((data.status() * 100) / data.maxStatus()) + "%"));
                textConsumer.accept(Text.literal(Text.translatable("fish-tanks.filter_bag.filter_strength").getString() + data.strength()));
            }
        }
    }
}
