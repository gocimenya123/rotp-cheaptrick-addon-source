package com.cheaptrick.addon.logic;

import java.util.UUID;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.config.CheapTrickConfig;
import com.cheaptrick.addon.data.CheapTrickData;
import com.cheaptrick.addon.net.CheapTrickNetwork;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

/** Серверные события (шина Forge). Один обработчик тика на весь аддон; тяжёлая работа — по интервалам. */
@Mod.EventBusSubscriber(modid = CheapTrickAddon.MOD_ID)
public class ServerEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !CheapTrickConfig.enabled()) {
            return;
        }
        MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int tick = server.getTickCount();

        // Сверка выдачи стрелой / дубликатов — раз в 2 секунды
        if (tick % 40 == 0) {
            for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
                CheapTrickManager.reconcilePlayer(server, p);
            }
        }

        ServerPlayerEntity carrier = CheapTrickManager.getOnlineCarrier(server);
        if (carrier == null) {
            return; // носитель оффлайн / отсутствует: механики проклятия не работают, ванильное поведение не затронуто
        }
        long now = server.overworld().getGameTime();

        if (carrier.isAlive() && !carrier.isSpectator()) {
            // 1) Взгляд на спину
            if (tick % CheapTrickConfig.gazeCheckInterval() == 0 && !CheapTrickManager.isInGrace(carrier.getUUID(), now)) {
                BackGazeDetector.Result result = BackGazeDetector.detect(carrier);
                if (result != null) {
                    if (result.player != null) {
                        CheapTrickManager.transferByPlayerGaze(server, carrier, result.player);
                    } else if (result.mob != null) {
                        CheapTrickManager.killByMobGaze(server, carrier);
                    }
                }
            }
            // 2) Враждебность мобов
            if (tick % CheapTrickConfig.hostilityInterval() == 0) {
                MobAggression.tick(server, carrier);
            }
        }
        // 3) Реплики и шёпот — раз в секунду
        if (tick % 20 == 0) {
            CheapTrickChat.tickPhrases(server, carrier, now);
            CheapTrickChat.tickWhispers(server, carrier, now);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            MinecraftServer server = player.getServer();
            if (server != null) {
                CheapTrickNetwork.sendTo(player, CheapTrickData.get(server).getCarrier());
            }
            CheapTrickChat.forgetPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CheapTrickChat.forgetPlayer(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            MinecraftServer server = player.getServer();
            if (server != null) {
                long now = server.overworld().getGameTime();
                CheapTrickManager.setGrace(player.getUUID(), now + CheapTrickConfig.respawnGraceTicks());
                // Для новой сущности игрока клиентам достаточно прежнего UUID; повторно шлём для надёжности
                CheapTrickNetwork.sendToAll(CheapTrickData.get(server).getCarrier());
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            MinecraftServer server = player.getServer();
            if (server != null) {
                CheapTrickNetwork.sendTo(player, CheapTrickData.get(server).getCarrier());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayerEntity && CheapTrickConfig.debug()) {
            UUID id = event.getEntityLiving().getUUID();
            MinecraftServer server = ((ServerPlayerEntity) event.getEntityLiving()).getServer();
            if (server != null && CheapTrickData.get(server).isCarrier(id)) {
                CheapTrickAddon.debug("Носитель {} погиб ({}), стенд остаётся у него", event.getEntityLiving().getName().getString(),
                        event.getSource().msgId);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        CheapTrickManager.resetTransientState();
        CheapTrickChat.resetAll();
    }

    @SubscribeEvent
    public static void onServerStopped(FMLServerStoppedEvent event) {
        CheapTrickManager.resetTransientState();
        CheapTrickChat.resetAll();
    }
}
