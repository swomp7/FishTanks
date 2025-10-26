package fish.tanks.registries;

import fish.tanks.FishTanks;
import fish.tanks.screens.FilterData;
import fish.tanks.screens.FilterScreenHandler;
import fish.tanks.screens.FishTankScreenHandler;
import fish.tanks.screens.TankData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class FishTankScreens {
    
    public static final ScreenHandlerType<FishTankScreenHandler> FISH_TANK_SCREEN_HANDLER = register("fish_tank_screen_handler", new ExtendedScreenHandlerType<>(FishTankScreenHandler::new, TankData.PACKET_CODEC));
    public static final ScreenHandlerType<FilterScreenHandler> FILTER_SCREEN_HANDLER = register("filter_screen_handler", new ExtendedScreenHandlerType<>(FilterScreenHandler::new, FilterData.PACKET_CODEC));

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String name, ExtendedScreenHandlerType<T, ?> entry) {
        return Registry.register(Registries.SCREEN_HANDLER, Identifier.of(FishTanks.MOD_ID, name), entry);
    }

    public static void register() {}
}
