package com.cheaptrick.addon.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.cheaptrick.addon.config.CheapTrickConfig;
import com.cheaptrick.addon.text.CheapTrickPhrases;
import com.cheaptrick.addon.text.CheapTrickPhrases.Context;
import com.cheaptrick.addon.text.CheapTrickWhispers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * Случайные реплики носителю и редкие личные сообщения другим игрокам.
 * Всё считается на сервере по игровому времени; состояние таймеров временное (после перезапуска таймеры
 * просто переинициализируются случайными значениями).
 */
public final class CheapTrickChat {
    private CheapTrickChat() {}

    private static final Random RANDOM = new Random();

    private static final Map<UUID, Long> NEXT_PHRASE = new HashMap<>();
    private static final Map<UUID, Deque<String>> RECENT = new HashMap<>();
    private static final Deque<String> RECENT_WHISPERS = new ArrayDeque<>();
    private static final Map<UUID, Long> RECIPIENT_COOLDOWN_UNTIL = new HashMap<>();
    private static long nextWhisperTick = -1;
    private static long whisperGlobalCooldownUntil = 0;
    private static long lastAnyMessageTick = Long.MIN_VALUE / 2;

    public static void resetAll() {
        NEXT_PHRASE.clear();
        RECENT.clear();
        RECENT_WHISPERS.clear();
        RECIPIENT_COOLDOWN_UNTIL.clear();
        nextWhisperTick = -1;
        whisperGlobalCooldownUntil = 0;
        lastAnyMessageTick = Long.MIN_VALUE / 2;
    }

    /** Сбросить таймер реплик игрока (вход, выход, смена носителя) — после этого он получит случайную паузу. */
    public static void forgetPlayer(UUID id) {
        NEXT_PHRASE.remove(id);
    }

    private static long randomTicks(int minSeconds, int maxSeconds) {
        int lo = Math.min(minSeconds, maxSeconds), hi = Math.max(minSeconds, maxSeconds);
        return 20L * (lo + RANDOM.nextInt(hi - lo + 1));
    }

    // ------------------------------------------------------------------ реплики носителю

    /** Вызывается раз в секунду для онлайн-носителя. */
    public static void tickPhrases(MinecraftServer server, ServerPlayerEntity carrier, long now) {
        if (!carrier.isAlive()) {
            return;
        }
        Long next = NEXT_PHRASE.get(carrier.getUUID());
        if (next == null) {
            NEXT_PHRASE.put(carrier.getUUID(), now + randomTicks(CheapTrickConfig.phraseMinSeconds(), CheapTrickConfig.phraseMaxSeconds()));
            return;
        }
        if (now < next) {
            return;
        }
        if (now - lastAnyMessageTick < 20L * CheapTrickConfig.globalMessageCooldownSeconds()) {
            NEXT_PHRASE.put(carrier.getUUID(), now + 100); // общий кулдаун — повторить попытку чуть позже
            return;
        }
        String text = pickPhrase(carrier);
        IFormattableTextComponent msg = prefix("<Cheap Trick> ").append(new StringTextComponent(text).withStyle(TextFormatting.WHITE));
        if (CheapTrickConfig.globalChat()) {
            server.getPlayerList().broadcastMessage(msg, ChatType.SYSTEM, Util.NIL_UUID);
        } else {
            carrier.sendMessage(msg, ChatType.SYSTEM, Util.NIL_UUID);
        }
        lastAnyMessageTick = now;
        NEXT_PHRASE.put(carrier.getUUID(), now + randomTicks(CheapTrickConfig.phraseMinSeconds(), CheapTrickConfig.phraseMaxSeconds()));
    }

    private static String pickPhrase(ServerPlayerEntity carrier) {
        Deque<String> recent = RECENT.computeIfAbsent(carrier.getUUID(), k -> new ArrayDeque<>());
        List<Context> contexts = detectContexts(carrier);
        String chosen = null;
        for (int attempt = 0; attempt < 12 && chosen == null; attempt++) {
            Context ctx = (!contexts.isEmpty() && RANDOM.nextInt(100) < 55)
                    ? contexts.get(RANDOM.nextInt(contexts.size())) : Context.GENERIC;
            List<String> pool = CheapTrickPhrases.get(ctx);
            String candidate = pool.get(RANDOM.nextInt(pool.size()));
            if (!recent.contains(candidate)) {
                chosen = candidate;
            }
        }
        if (chosen == null) { // всё недавно говорилось — берём любую общую
            List<String> pool = CheapTrickPhrases.get(Context.GENERIC);
            chosen = pool.get(RANDOM.nextInt(pool.size()));
        }
        recent.addLast(chosen);
        while (recent.size() > Math.max(CheapTrickConfig.phraseMemory(), 0)) {
            recent.pollFirst();
        }
        return chosen;
    }

    private static List<Context> detectContexts(ServerPlayerEntity p) {
        List<Context> out = new ArrayList<>();
        World w = p.level;
        BlockPos pos = p.blockPosition();
        if (w.isNight()) out.add(Context.NIGHT);
        if (w.isRaining() && w.canSeeSky(pos)) out.add(Context.RAIN);
        if (pos.getY() < 55 && !w.canSeeSky(pos)) out.add(Context.CAVE);
        if (p.isInWater() || p.isSwimming()) out.add(Context.WATER);
        if (p.isSprinting() || p.isFallFlying()) out.add(Context.TRAVEL);
        if (p.getHealth() <= 6.0F) out.add(Context.LOW_HEALTH);
        if (p.tickCount - p.getLastHurtByMobTimestamp() < 200 && p.getLastHurtByMobTimestamp() != 0) out.add(Context.COMBAT);
        List<PlayerEntity> near = w.getEntitiesOfClass(PlayerEntity.class, p.getBoundingBox().inflate(48.0D),
                o -> o != p && !o.isSpectator());
        if (!near.isEmpty()) {
            boolean close = false;
            for (PlayerEntity o : near) {
                if (o.distanceToSqr(p) < 24.0D * 24.0D) { close = true; break; }
            }
            out.add(close ? Context.OTHERS_NEAR : Context.ALONE);
        } else {
            out.add(Context.ALONE);
        }
        return out;
    }

    // ------------------------------------------------------------------ личные сообщения с координатами

    /** Вызывается раз в секунду. */
    public static void tickWhispers(MinecraftServer server, ServerPlayerEntity carrier, long now) {
        if (nextWhisperTick < 0) {
            nextWhisperTick = now + randomTicks(CheapTrickConfig.whisperMinMinutes() * 60, CheapTrickConfig.whisperMaxMinutes() * 60);
            return;
        }
        if (now < nextWhisperTick) {
            return;
        }
        // Таймер истёк: событие сработает с вероятностью chance, иначе переносится на ~минуту
        if (RANDOM.nextDouble() >= CheapTrickConfig.whisperChance()) {
            nextWhisperTick = now + 20L * 60;
            return;
        }
        if (!carrier.isAlive() || !CheapTrickManager.isOnline(carrier)) {
            nextWhisperTick = now + 20L * 30;
            return;
        }
        if (now < whisperGlobalCooldownUntil || now - lastAnyMessageTick < 20L * CheapTrickConfig.globalMessageCooldownSeconds()) {
            nextWhisperTick = now + 20L * 30;
            return;
        }
        List<ServerPlayerEntity> eligible = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(carrier.getUUID()) || !p.isAlive() || p.isSpectator() || !CheapTrickManager.isOnline(p)) continue;
            Long until = RECIPIENT_COOLDOWN_UNTIL.get(p.getUUID());
            if (until != null && now < until) continue;
            eligible.add(p);
        }
        if (eligible.isEmpty()) { // один игрок на сервере или никто не подходит — пропускаем, попробуем позже
            nextWhisperTick = now + 20L * 120;
            return;
        }
        Collections.shuffle(eligible, RANDOM);
        int count = Math.min(CheapTrickConfig.whisperMaxRecipients(), eligible.size());
        BlockPos cpos = carrier.blockPosition();
        int x = cpos.getX(), y = cpos.getY(), z = cpos.getZ();
        String template = pickWhisper();
        for (int i = 0; i < count; i++) {
            ServerPlayerEntity target = eligible.get(i);
            String body = CheapTrickWhispers.format(template, x, y, z);
            if (!target.level.dimension().equals(carrier.level.dimension())) {
                body += " (" + carrier.level.dimension().location() + ")";
            }
            // Сообщение уходит только выбранному получателю — координаты не видит никто другой
            target.sendMessage(prefix("<Cheap Trick> ")
                    .append(new StringTextComponent(body).withStyle(TextFormatting.WHITE)),
                    ChatType.SYSTEM, Util.NIL_UUID);
            RECIPIENT_COOLDOWN_UNTIL.put(target.getUUID(), now + 20L * CheapTrickConfig.whisperRecipientCooldownSeconds());
        }
        lastAnyMessageTick = now;
        whisperGlobalCooldownUntil = now + 20L * CheapTrickConfig.whisperGlobalCooldownSeconds();
        nextWhisperTick = now + randomTicks(CheapTrickConfig.whisperMinMinutes() * 60, CheapTrickConfig.whisperMaxMinutes() * 60);
    }

    private static String pickWhisper() {
        List<String> all = CheapTrickWhispers.all();
        String chosen = all.get(RANDOM.nextInt(all.size()));
        for (int i = 0; i < 10 && RECENT_WHISPERS.contains(chosen); i++) {
            chosen = all.get(RANDOM.nextInt(all.size()));
        }
        RECENT_WHISPERS.addLast(chosen);
        while (RECENT_WHISPERS.size() > 8) RECENT_WHISPERS.pollFirst();
        return chosen;
    }

    private static IFormattableTextComponent prefix(String s) {
        return new StringTextComponent(s).withStyle(TextFormatting.WHITE);
    }
}
