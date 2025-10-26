package fish.tanks.networking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import fish.tanks.tank.FishStatus;
import fish.tanks.tank.FishStatusDataCarrier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

// Sync the lists in a tank
public record SyncListsPacket(List<UUID> spawned, List<Integer> spawnedElements, List<ItemStack> buckets, List<UUID> alreadySpawned, List<Integer> alreadySpawnedElements, List<FishStatusDataCarrier> statuses, List<Integer> statusesElements, List<ItemStack> inventory, BlockPos pos) implements CustomPayload {
    
    public static final Id<SyncListsPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_lists_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncListsPacket> PACKET = PacketCodec.tuple(Uuids.PACKET_CODEC.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::spawned, PacketCodecs.INTEGER.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::spawnedElements, ItemStack.OPTIONAL_LIST_PACKET_CODEC, SyncListsPacket::buckets, Uuids.PACKET_CODEC.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::alreadySpawned, PacketCodecs.INTEGER.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::alreadySpawnedElements, FishStatusDataCarrier.PACKET_CODEC.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::statuses, PacketCodecs.INTEGER.collect(PacketCodecs.toCollection(DefaultedList::ofSize)), SyncListsPacket::statusesElements, ItemStack.OPTIONAL_LIST_PACKET_CODEC, SyncListsPacket::inventory, BlockPos.PACKET_CODEC, SyncListsPacket::pos, SyncListsPacket::new);

    public static void receive(SyncListsPacket payload, ClientPlayNetworking.Context context) {
        List<UUID> spawned = payload.spawned;
        List<Integer> spawnedElements = payload.spawnedElements;
        List<ItemStack> buckets = payload.buckets;
        List<UUID> alreadySpawned = payload.alreadySpawned;
        List<Integer> alreadySpawnedElements = payload.alreadySpawnedElements;
        List<FishStatusDataCarrier> statuses = payload.statuses;
        List<Integer> statusesElements = payload.statusesElements;
        List<ItemStack> inventory = payload.inventory;
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            DefaultedList<Optional<UUID>> dSpawned = DefaultedList.ofSize(spawnedElements.getLast(), Optional.empty());
            DefaultedList<ItemStack> dBuckets = DefaultedList.ofSize(buckets.size(), ItemStack.EMPTY);
            DefaultedList<Optional<UUID>> dAlreadySpawned = DefaultedList.ofSize(alreadySpawnedElements.getLast(), Optional.empty());
            DefaultedList<Optional<FishStatus>> dStatuses = DefaultedList.ofSize(statusesElements.getLast(), Optional.empty());
            DefaultedList<ItemStack> dInventory = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);

            for (int i = 0; i < spawned.size(); i++) dSpawned.set(spawnedElements.get(i), Optional.of(spawned.get(i)));
            for (int i = 0; i < buckets.size(); i++) dBuckets.set(i, buckets.get(i));
            for (int i = 0; i < alreadySpawned.size(); i++) dAlreadySpawned.set(alreadySpawnedElements.get(i), Optional.of(alreadySpawned.get(i)));
            for (int i = 0; i < statuses.size(); i++) {
                FishStatusDataCarrier status = statuses.get(i);
                statusesElements.get(i);
                alreadySpawned.get(i);
                dStatuses.set(statusesElements.get(i), Optional.of(new FishStatus(status.health(), status.maxHealth(), status.hunger(), status.happiness(), alreadySpawned.get(i))));
            }
            for (int i = 0; i < inventory.size(); i++) dInventory.set(i, inventory.get(i));

            tank.setLists(dSpawned, dBuckets, dAlreadySpawned, dStatuses, dInventory);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
