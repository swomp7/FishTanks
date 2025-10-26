package fish.tanks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fish.tanks.items.TankBuilderItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

// Used to allow the Tank Builder Item's mode rotation in survival
@Mixin(ServerPlayNetworkHandler.class)
public abstract class RotateModeMixin {

    @Accessor("player")
    abstract ServerPlayerEntity player();
    
    @Inject(method = "onPlayerAction", at = @At(value = "JUMP", ordinal = 0))
    private void onAbortDestroyBlock(PlayerActionC2SPacket packet, CallbackInfo cb) {
        if (packet.getAction().equals(Action.START_DESTROY_BLOCK)) {
            if (player().getMainHandStack().getItem() instanceof TankBuilderItem builder && builder.canRotate()) {
                builder.rotate(player());
            }
        }
    }
}
