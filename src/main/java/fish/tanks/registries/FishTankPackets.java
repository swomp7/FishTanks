package fish.tanks.registries;

import fish.tanks.networking.RemoveTankPacket;
import fish.tanks.networking.ScrollPacket;
import fish.tanks.networking.SyncBlocksPacket;
import fish.tanks.networking.SyncBooleanPacketC2S;
import fish.tanks.networking.SyncBooleanPacketS2C;
import fish.tanks.networking.SyncIntPacketS2C;
import fish.tanks.networking.SyncListsPacket;
import fish.tanks.networking.SyncPlantPacket;
import fish.tanks.networking.SyncScreenPacket;
import fish.tanks.networking.SyncTankCleaningPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

// Handles all of the syncing between the server and the client (see respective classes)
public class FishTankPackets {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ScrollPacket.ID, ScrollPacket.PACKET);
        PayloadTypeRegistry.playC2S().register(SyncScreenPacket.ID, SyncScreenPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncBlocksPacket.ID, SyncBlocksPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncPlantPacket.ID, SyncPlantPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncTankCleaningPacket.ID, SyncTankCleaningPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncListsPacket.ID, SyncListsPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncBooleanPacketS2C.ID, SyncBooleanPacketS2C.PACKET);
        PayloadTypeRegistry.playC2S().register(SyncBooleanPacketC2S.ID, SyncBooleanPacketC2S.PACKET);
        PayloadTypeRegistry.playS2C().register(RemoveTankPacket.ID, RemoveTankPacket.PACKET);
        PayloadTypeRegistry.playS2C().register(SyncIntPacketS2C.ID, SyncIntPacketS2C.PACKET);

        ServerPlayNetworking.registerGlobalReceiver(ScrollPacket.ID, ScrollPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(SyncScreenPacket.ID, SyncScreenPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(SyncBooleanPacketC2S.ID, SyncBooleanPacketC2S::receive);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncBlocksPacket.ID, SyncBlocksPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(SyncPlantPacket.ID, SyncPlantPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(SyncTankCleaningPacket.ID, SyncTankCleaningPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(SyncListsPacket.ID, SyncListsPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(SyncBooleanPacketS2C.ID, SyncBooleanPacketS2C::receive);
        ClientPlayNetworking.registerGlobalReceiver(RemoveTankPacket.ID, RemoveTankPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(SyncIntPacketS2C.ID, SyncIntPacketS2C::receive);
    }
}
