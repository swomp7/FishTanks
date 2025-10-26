package fish.tanks.tank;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

// Status of a fish in a tank - expanded version
public class FishStatus {

    public static final Codec<FishStatus> CODEC = RecordCodecBuilder.create(in -> in.group(
        Codec.FLOAT.fieldOf("health").forGetter(s -> s.getHealth()),
        Codec.FLOAT.fieldOf("maxHealth").forGetter(s -> s.getMaxHealth()),
        Codec.FLOAT.fieldOf("hunger").forGetter(s -> s.getHunger()),
        Codec.INT.fieldOf("happiness").forGetter(s -> s.getHappiness()),
        Uuids.CODEC.fieldOf("entity").forGetter(s -> s.getEntity())
    ).apply(in, FishStatus::new));
    public static final Codec<FishDescription> DESC_CODEC = Codec.STRING.xmap(str -> FishDescription.valueOf(str), value -> value.toString());
    public static final PacketCodec<RegistryByteBuf, FishDescription> DESC_PACKET_CODEC = new PacketCodec<RegistryByteBuf,FishStatus.FishDescription>() {

        @Override
        public FishDescription decode(RegistryByteBuf buf) {
            return FishDescription.valueOf(buf.readString());
        }

        @Override
        public void encode(RegistryByteBuf buf, FishDescription value) {
            buf.writeString(value.toString());
        }
        
    };

    private float health;
    private final float maxHealth;
    private float hunger;
    private float maxHunger = 10;
    private final UUID entity;
    private FishDescription desc;
    private int happiness = 50;
    private int maxHappiness = 50;
    private int dirtyDamageCountdown = 50, hungerDamageCountdown = 30, starvingCountdown = 90, damageCountdown = 300;
    private int happinessRiseCountdown = 6, healCountdown = 150, hungerRiseCountdown = 25;

    public FishStatus(float health, float maxHealth, float hunger, int happiness, UUID entity) {
        this.entity = entity;
        this.health = health;
        this.maxHealth = maxHealth;
        this.hunger = hunger;
        this.happiness = happiness;
        updateEntity();
    }

    public void updateHealth(float health, Entity mob) {
        this.health = health;
        if (mob instanceof LivingEntity livingEntity) livingEntity.setHealth(health);
    }

    public float getHealth() {
        return health;
    }

    public float getHunger() {
        return hunger;
    }

    public float getMaxHealth() {
        return maxHealth;
    }
    
    public float getMaxHunger() {
        return maxHunger;
    }

    public int getHappiness() {
        return happiness;
    }

    public UUID getEntity() {
        return entity;
    }

    /** returns true if the entity is alive, false if not */
    public boolean updateEntity() {
        if (desc == FishDescription.HEALING) return true;

        if (health <= 0) setStatus(FishDescription.DEAD);
        else if (health < maxHealth) setStatus(FishDescription.HURT);
        else if (hunger <= 5) setStatus(FishDescription.HUNGRY);
        else setStatus(FishDescription.ALIVE);

        return desc != FishDescription.DEAD;
    }

    /** returns true if the entity is alive, false if not */
    public boolean tickEntity(boolean hasFood, boolean isTankDirty, int tickCounter, int hasDecor, World world) {
        if (hasDecor == 0) maxHappiness = 50;
        else if (hasDecor == 1) maxHappiness = 75;
        else maxHappiness = 100;

        Entity entity = world.getEntity(this.entity);
        if (entity == null) return false;
        LivingEntity mob = (LivingEntity)entity;
        if (mob.getHealth() != health) health = mob.getHealth();

        if (isTankDirty && happiness > 0) {
            if (dirtyDamageCountdown > 0) dirtyDamageCountdown--;
            else {
                dirtyDamageCountdown = 50;
                happiness--;
            }
        }

        if (!hasFood) {
            if (hunger > 0) {
                if (starvingCountdown > 0) starvingCountdown--;
                else {
                    starvingCountdown = 90;
                    hunger -= 0.25F;
                }
            }
            if (hunger <= hunger / 4 && happiness > 0) {
                if (hungerDamageCountdown > 0) hungerDamageCountdown--;
                else {
                    hungerDamageCountdown = 30;
                    happiness--;
                }
            }
        } else if (hunger < maxHunger) {
            if (hungerRiseCountdown > 0) hungerRiseCountdown--;
            else {
                hungerRiseCountdown = 25;
                hunger += 0.25F;
            }
        }

        if (happiness < 10 && health > 0) {
            if (damageCountdown > 0) damageCountdown--;
            else {
                damageCountdown = 300;
                health -= 0.25F;
            }
        } else if (health < maxHealth && happiness >= 75) {
            if (healCountdown > 0) healCountdown--;
            else {
                healCountdown = 150;
                recover(mob);
            }
        } else if (health == maxHealth && desc.equals(FishDescription.HEALING)) setStatus(FishDescription.ALIVE);

        if (hasFood && !isTankDirty && happiness < maxHappiness) {
            if (happinessRiseCountdown > 0) happinessRiseCountdown--;
            else {
                happinessRiseCountdown = 6;
                happiness++;
            }
        }

        if (happiness < 0) happiness = 0;
        else if (happiness > 100) happiness = 100;

        if (health < 0) health = 0;
        else if (health > maxHealth) health = maxHealth;

        if (hunger < 0) hunger = 0;
        else if (hunger > maxHunger) hunger = maxHunger;

        mob.setHealth(health);
        return updateEntity();
    }

    public void setStatus(FishDescription desc) {
        this.desc = desc;
    }

    public FishDescription getStatus() {
        return desc;
    }

    public boolean recover(Entity mob) {
        health += 0.5;
        if (health > maxHealth) health = maxHealth;
        if (mob instanceof LivingEntity livingEntity) livingEntity.setHealth(health);
    
        if (health == maxHealth) {
            setStatus(FishDescription.ALIVE);
            return false;
        }
        setStatus(FishDescription.HEALING);
        return true;
    }

    public enum FishDescription {
        MISSING(Formatting.DARK_RED), DEAD(Formatting.DARK_RED), ALIVE(Formatting.DARK_GREEN), HUNGRY(Formatting.YELLOW), HURT(Formatting.RED), HEALING(Formatting.GOLD);

        private final Formatting format;

        private FishDescription(Formatting format) {
            this.format = format;
        }

        public Text toText() {
            String translationKey = "fish-tanks.fish_tank_status." + this.toString().toLowerCase();
            return Text.translatable(translationKey).formatted(format);
        }
    }
}
