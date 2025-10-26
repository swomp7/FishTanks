package fish.tanks.recipes;

import java.util.List;

import fish.tanks.registries.FishTankComponents;
import fish.tanks.registries.FishTankItems;
import fish.tanks.registries.FishTankRecipes;
import fish.tanks.util.FishTankTags;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.world.World;

// Scalable food bag recipe to add number of food items to the bag
public class MakeFoodBagRecipe extends SpecialCraftingRecipe {

    private int foodCount = 0, bagCount = 0, bagSlot = 0;

    public MakeFoodBagRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getStackCount() < 2) return false;
        setCounts(input);
        return bagCount == 1 && foodCount >= 1 && bagSlot != -1;
    }

    private void setCounts(CraftingRecipeInput input) {
        List<ItemStack> stacks = input.getStacks();
        bagCount = 0;
        foodCount = 0;
        bagSlot = -1;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.getItem().equals(FishTankItems.FISH_FOOD) || stack.getItem().equals(Items.BUNDLE)) {
                bagCount++;
                bagSlot = i;
            } else if (stack.isIn(FishTankTags.FISH_FOODS)) foodCount++;
            else if (!stack.isEmpty()) {
                foodCount = -1;
                break;
            }
        }
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, WrapperLookup registries) {
        setCounts(input);
        if (foodCount < 1 || bagCount < 1 || bagSlot == -1) return ItemStack.EMPTY;
        List<ItemStack> stacks = input.getStacks();
        ItemStack bag = stacks.get(bagSlot);
        ItemStack result = new ItemStack(FishTankItems.FISH_FOOD);
        if (bag.contains(FishTankComponents.FOOD_STATUS) && bag.getItem().equals(FishTankItems.FISH_FOOD)) result.set(FishTankComponents.FOOD_STATUS, bag.get(FishTankComponents.FOOD_STATUS) + foodCount >= 64 ? 64 : bag.get(FishTankComponents.FOOD_STATUS) + foodCount);
        else result.set(FishTankComponents.FOOD_STATUS, foodCount);
        int status = result.get(FishTankComponents.FOOD_STATUS);
        if (status <= 16) result.set(FishTankComponents.PARTIALLY_FILLED, true);
        else if (status <= 32) result.set(FishTankComponents.HALF_FILLED, true);
        else if (status <= 48) result.set(FishTankComponents.MOSTLY_FILLED, true);
        return result;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return FishTankRecipes.FOOD_BAG_RECIPE;
    }
}
