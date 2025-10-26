package fish.tanks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.EntityBucketItem;

// For compatibility with other mods: get the entity type of any fish bucket
@Mixin(EntityBucketItem.class)
public interface FishGetterMixin {
    
    @Accessor("entityType")
    EntityType<? extends MobEntity> entityType();
}
