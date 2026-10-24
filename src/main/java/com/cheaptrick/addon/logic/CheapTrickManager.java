package com.cheaptrick.addon.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.config.CheapTrickConfig;
import com.cheaptrick.addon.data.CheapTrickData;
import com.cheaptrick.addon.init.InitStands;
import com.cheaptrick.addon.net.CheapTrickNetwork;
import com.cheaptrick.addon.util.CheapTrickDamage;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * Единая точка изменения состояния носителя. Всё выполняется в серверном потоке,
 * атомарность обеспечивается порядком операций, флагом {@link #transferring} и проверкой тика последней передачи.
 */
public final class CheapTrickManager {
    private CheapTrickManager() {}

    /** Временные (не требующие сохранения) данные: защитные паузы. Сбрасываются при остановке сервера. */
    private static final Map<UUID, Long> GRACE_UNTIL = new HashMap<>();
    private static long lastTransferTick = Long.MIN_VALUE;
    private static boolean transferring;

    public static void resetTransientState() {
        GRACE_UNTIL.clear();
        lastTransferTick = Long.MIN_VALUE;
        transferring = false;
    }

    // ------------------------------------------------------------------ запросы

    /** Игрок подключён и может получать сообщения. */
    public static boolean isOnline(ServerPlayerEntity player) {
        return player.connection != null && player.connection.connection.isConnected();
    }

    public static boolean isCarrier(LivingEntity entity) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && entity != null && CheapTrickData.get(server).isCarrier(entity.getUUID());
    }

    @Nullable
    public static ServerPlayerEntity getOnlineCarrier(MinecraftServer server) {
        UUID id = CheapTrickData.get(server).getCarrier();
        return id == null ? null : server.getPlayerList().getPlayer(id);
    }

    public static boolean isInGrace(UUID player, long gameTime) {
        Long until = GRACE_UNTIL.get(player);
        return until != null && gameTime < until;
    }

    public static void setGrace(UUID player, long untilGameTime) {
        GRACE_UNTIL.put(player, untilGameTime);
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // ------------------------------------------------------------------ передача при взгляде игрока

    /**
     * Атомарная передача: сначала фиксируется новое состояние в хранилище, затем старый носитель умирает.
     * Повторный вызов в том же тике или во время передачи игнорируется.
     */
    public static boolean transferByPlayerGaze(MinecraftServer server, ServerPlayerEntity oldCarrier, ServerPlayerEntity observer) {
        if (transferring) {
            return false;
        }
        long now = now(server);
        if (lastTransferTick == now) {
            return false;
        }
        CheapTrickData data = CheapTrickData.get(server);
        if (!data.isCarrier(oldCarrier.getUUID()) || oldCarrier.getUUID().equals(observer.getUUID())) {
            return false; // носитель уже сменился
        }
        if (!observer.isAlive() || observer.isSpectator() || !isOnline(observer)) {
            return false; // наблюдатель умер / вышел — передача не происходит, носитель не гибнет
        }
        boolean observerHasOtherStand = hasOtherStand(observer);
        if (observerHasOtherStand && !CheapTrickConfig.coexist()) {
            return false;
        }

        transferring = true;
        try {
            lastTransferTick = now;
            // 1) Состояние: новый носитель фиксируется ДО смерти старого — стенд не может «потеряться».
            data.setCarrier(observer.getUUID(), observer.getGameProfile().getName(), true);
            setGrace(observer.getUUID(), now + CheapTrickConfig.transferGraceTicks());
            setGrace(oldCarrier.getUUID(), now + CheapTrickConfig.transferGraceTicks());
            // 2) Зеркало в системе стендов RotP
            removeMirror(oldCarrier);
            giveMirror(observer);
            // 3) Клиенты: модель исчезает со старой спины и появляется на новой
            CheapTrickNetwork.sendToAll(observer.getUUID());
            // 4) Смерть прежнего носителя
            killByCurse(oldCarrier);
            tell(observer, "Ты увидел его спину. Теперь она — твоя.");
            CheapTrickAddon.debug("Передача Cheap Trick: {} -> {}", oldCarrier.getGameProfile().getName(), observer.getGameProfile().getName());
            return true;
        } finally {
            transferring = false;
        }
    }

    // ------------------------------------------------------------------ спину увидел моб

    /** Носитель умирает, стенд остаётся у него, моб не затрагивается. */
    public static boolean killByMobGaze(MinecraftServer server, ServerPlayerEntity carrier) {
        if (transferring) {
            return false;
        }
        long now = now(server);
        if (lastTransferTick == now || !CheapTrickData.get(server).isCarrier(carrier.getUUID())) {
            return false;
        }
        transferring = true;
        try {
            lastTransferTick = now;
            // Защита от повторного срабатывания на том же наблюдении (в т.ч. сразу после возрождения)
            setGrace(carrier.getUUID(), now + Math.max(CheapTrickConfig.respawnGraceTicks(), 20));
            killByCurse(carrier);
            CheapTrickAddon.debug("Моб увидел спину носителя {}: носитель убит, стенд остаётся", carrier.getGameProfile().getName());
            return true;
        } finally {
            transferring = false;
        }
    }

    private static void killByCurse(ServerPlayerEntity victim) {
        if (!victim.isAlive()) {
            return;
        }
        victim.hurt(CheapTrickDamage.INSTANCE, Float.MAX_VALUE);
        if (victim.isAlive()) { // запасной путь, если урон был перехвачен другим модом
            victim.setHealth(0.0F);
            victim.die(CheapTrickDamage.INSTANCE);
        }
    }

    // ------------------------------------------------------------------ административное назначение / стрела

    /** Назначает носителя без убийства (команда /cheaptrick set и получение стрелой). */
    public static void assign(MinecraftServer server, @Nullable ServerPlayerEntity newCarrier) {
        CheapTrickData data = CheapTrickData.get(server);
        UUID oldId = data.getCarrier();
        if (oldId != null) {
            ServerPlayerEntity old = server.getPlayerList().getPlayer(oldId);
            if (old != null && (newCarrier == null || !old.getUUID().equals(newCarrier.getUUID()))) {
                removeMirror(old);
            }
        }
        if (newCarrier == null) {
            data.setCarrier(null, "", false);
            CheapTrickNetwork.sendToAll(null);
            return;
        }
        data.setCarrier(newCarrier.getUUID(), newCarrier.getGameProfile().getName(), false);
        setGrace(newCarrier.getUUID(), now(server) + CheapTrickConfig.transferGraceTicks());
        giveMirror(newCarrier);
        CheapTrickNetwork.sendToAll(newCarrier.getUUID());
    }

    /**
     * Периодическая сверка: игрок получил Cheap Trick из стрелы (или иным способом через RotP) —
     * регистрируем его носителем; если носитель уже есть, лишний экземпляр удаляется (стенд уникален).
     */
    public static void reconcilePlayer(MinecraftServer server, ServerPlayerEntity player) {
        if (transferring) {
            return;
        }
        CheapTrickData data = CheapTrickData.get(server);
        IStandPower.getStandPowerOptional(player).ifPresent(power -> {
            boolean hasCheapTrickStand = power.hasPower() && power.getType() == InitStands.CHEAP_TRICK.get();
            boolean isCarrier = data.isCarrier(player.getUUID());
            if (hasCheapTrickStand && !isCarrier) {
                if (!data.hasCarrier() && CheapTrickConfig.enabled()) {
                    assign(server, player);
                    tell(player, "Я теперь с тобой. Всегда.");
                } else {
                    power.clear(); // дубликат
                    CheapTrickAddon.debug("Лишний Cheap Trick удалён у {}", player.getGameProfile().getName());
                }
            } else if (isCarrier && !power.hasPower()) {
                giveMirror(player); // носитель без стенда: восстановить зеркало
            }
        });
    }

    // ------------------------------------------------------------------ зеркало в StandPower

    private static boolean hasOtherStand(ServerPlayerEntity player) {
        return IStandPower.getStandPowerOptional(player)
                .map(p -> p.hasPower() && p.getType() != InitStands.CHEAP_TRICK.get()).orElse(false);
    }

    private static void giveMirror(ServerPlayerEntity player) {
        IStandPower.getStandPowerOptional(player).ifPresent(power -> {
            if (!power.hasPower()) {
                power.givePower(InitStands.CHEAP_TRICK.get());
            }
        });
    }

    private static void removeMirror(ServerPlayerEntity player) {
        IStandPower.getStandPowerOptional(player).ifPresent(power -> {
            if (power.hasPower() && power.getType() == InitStands.CHEAP_TRICK.get()) {
                power.clear();
            }
        });
    }

    private static void tell(ServerPlayerEntity player, String text) {
        player.sendMessage(new StringTextComponent("<Cheap Trick> ").withStyle(TextFormatting.WHITE)
                .append(new StringTextComponent(text).withStyle(TextFormatting.WHITE)),
                net.minecraft.util.text.ChatType.SYSTEM, net.minecraft.util.Util.NIL_UUID);
    }
}
