package fish.tanks.registries;

import fish.tanks.FishTanks;
import fish.tanks.recipes.MakeFoodBagRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FishTankRecipes {

    public static final RecipeSerializer<MakeFoodBagRecipe> FOOD_BAG_RECIPE = register("food_bag_recipe", new SpecialCraftingRecipe.SpecialRecipeSerializer(MakeFoodBagRecipe::new));
   
    private static <T extends Recipe<?>> RecipeSerializer<T> register(String name, RecipeSerializer<T> entry) {
        return Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(FishTanks.MOD_ID, name), entry);
    }

    public static void register() {}
}
