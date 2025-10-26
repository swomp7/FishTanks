package fish.tanks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fish.tanks.registries.*;

public class FishTanks implements ModInitializer {

	public static final String MOD_ID = "fish-tanks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FishTankBlocks.register();
		FishTankItems.register();
		FishTankGroup.register();
		FishTankScreens.register();
		FishTankPackets.register();
		FishTankComponents.register();
		FishTankRecipes.register();
	}

	public static <T> RegistryKey<T> makeKey(RegistryKey<? extends Registry<T>> key, String name) {
		return RegistryKey.of(key, Identifier.of(MOD_ID, name));
	}
}