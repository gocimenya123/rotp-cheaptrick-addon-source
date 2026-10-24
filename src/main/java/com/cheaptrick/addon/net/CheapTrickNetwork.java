package com.cheaptrick.addon.net;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.client.ClientState;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** Сетевая синхронизация: клиентам сообщается только UUID текущего носителя (для отрисовки модели). */
public final class CheapTrickNetwork {
    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    private CheapTrickNetwork() {}

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(CheapTrickAddon.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.registerMessage(0, CarrierPacket.class, CarrierPacket::encode, CarrierPacket::decode, CarrierPacket::handle);
    }

    public static void sendToAll(@Nullable UUID carrier) {
        if (channel != null) {
            channel.send(PacketDistributor.ALL.noArg(), new CarrierPacket(carrier));
        }
    }

    public static void sendTo(ServerPlayerEntity player, @Nullable UUID carrier) {
        if (channel != null) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), new CarrierPacket(carrier));
        }
    }

    public static class CarrierPacket {
        @Nullable
        private final UUID carrier;

        public CarrierPacket(@Nullable UUID carrier) {
            this.carrier = carrier;
        }

        public static void encode(CarrierPacket msg, PacketBuffer buf) {
            buf.writeBoolean(msg.carrier != null);
            if (msg.carrier != null) {
                buf.writeUUID(msg.carrier);
            }
        }

        public static CarrierPacket decode(PacketBuffer buf) {
            return new CarrierPacket(buf.readBoolean() ? buf.readUUID() : null);
        }

        public static void handle(CarrierPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.setCarrier(msg.carrier)));
            ctx.get().setPacketHandled(true);
        }
    }
}
