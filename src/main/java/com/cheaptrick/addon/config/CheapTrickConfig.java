package com.cheaptrick.addon.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.entity.EntityClassification;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * Общий (COMMON) конфиг аддона: config/cheaptrickaddon-common.toml.
 * Все геттеры безопасны: если конфиг ещё не загружен, возвращаются значения по умолчанию.
 */
public final class CheapTrickConfig {
    public static final ForgeConfigSpec SPEC;

    private static final BooleanValue ENABLED;
    private static final DoubleValue ARROW_WEIGHT;

    private static final DoubleValue GAZE_MAX_DISTANCE;
    private static final DoubleValue GAZE_BACK_HALF_ANGLE;
    private static final DoubleValue GAZE_AREA_WIDTH;
    private static final DoubleValue GAZE_AREA_HEIGHT;
    private static final DoubleValue GAZE_TOLERANCE;
    private static final DoubleValue MOB_GAZE_TOLERANCE;
    private static final IntValue GAZE_CHECK_INTERVAL;
    private static final IntValue TRANSFER_GRACE_TICKS;
    private static final IntValue RESPAWN_GRACE_TICKS;
    private static final BooleanValue IGNORE_CREATIVE_OBSERVERS;
    private static final BooleanValue COEXIST_WITH_OTHER_STANDS;

    private static final IntValue PHRASE_MIN_SECONDS;
    private static final IntValue PHRASE_MAX_SECONDS;
    private static final IntValue PHRASE_MEMORY;
    private static final BooleanValue GLOBAL_CHAT;
    private static final IntValue GLOBAL_MESSAGE_COOLDOWN_SECONDS;

    private static final IntValue WHISPER_MIN_MINUTES;
    private static final IntValue WHISPER_MAX_MINUTES;
    private static final DoubleValue WHISPER_CHANCE;
    private static final IntValue WHISPER_GLOBAL_COOLDOWN_SECONDS;
    private static final IntValue WHISPER_RECIPIENT_COOLDOWN_SECONDS;
    private static final IntValue WHISPER_MAX_RECIPIENTS;

    private static final BooleanValue HOSTILITY_ALL_PLAYERS;
    private static final BooleanValue HOSTILITY_AFFECTS_TAMED;
    private static final IntValue HOSTILITY_RADIUS;
    private static final IntValue HOSTILITY_INTERVAL;
    private static final ConfigValue<List<? extends String>> MOB_CATEGORIES;

    private static final BooleanValue VISUAL_MODEL;
    private static final BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Основные настройки").push("general");
        ENABLED = b.comment("Включить Cheap Trick. false — стенд не выдаётся стрелой, механики проклятия не работают.")
                .define("enabled", true);
        ARROW_WEIGHT = b.comment("Вес Cheap Trick в случайном выборе стенда стрелой (у большинства стендов RotP вес = 2.0).",
                "Чем больше — тем выше шанс. 0 — стрела никогда не выдаёт Cheap Trick.",
                "Пока носитель существует, Cheap Trick из пула стрелы исключается автоматически (стенд уникален).")
                .defineInRange("arrowWeight", 2.0D, 0.0D, 1000.0D);
        COEXIST_WITH_OTHER_STANDS = b.comment("Разрешить совместное владение: наблюдатель с другим стендом всё равно становится носителем Cheap Trick",
                "(его прежний стенд не затрагивается). Если false — такой наблюдатель не получает проклятие и носитель не умирает.")
                .define("coexistWithOtherStands", true);
        b.pop();

        b.comment("Обнаружение взгляда на спину (проверяется на сервере)").push("gaze");
        GAZE_MAX_DISTANCE = b.comment("Максимальная дистанция от глаз наблюдателя до спины, блоков.")
                .defineInRange("maxDistance", 24.0D, 2.0D, 128.0D);
        GAZE_BACK_HALF_ANGLE = b.comment("Полуугол конуса «позади носителя», градусы. 60 = наблюдатель в секторе 120° за спиной.")
                .defineInRange("backHalfAngleDegrees", 60.0D, 5.0D, 89.0D);
        GAZE_AREA_WIDTH = b.comment("Ширина области спины, блоков (ширина тела игрока 0.5).")
                .defineInRange("backAreaWidth", 0.6D, 0.2D, 1.5D);
        GAZE_AREA_HEIGHT = b.comment("Высота области спины от нижнего края торса, блоков (торс 0.75).")
                .defineInRange("backAreaHeight", 0.75D, 0.2D, 0.75D);
        GAZE_TOLERANCE = b.comment("Допуск попадания взгляда игрока в область спины, блоков.")
                .defineInRange("playerTolerance", 0.1D, 0.0D, 0.6D);
        MOB_GAZE_TOLERANCE = b.comment("Допуск для мобов (их голова поворачивается дискретно), блоков.")
                .defineInRange("mobTolerance", 0.3D, 0.0D, 1.0D);
        GAZE_CHECK_INTERVAL = b.comment("Как часто проверять взгляд, в тиках (1–20).")
                .defineInRange("checkIntervalTicks", 4, 1, 20);
        TRANSFER_GRACE_TICKS = b.comment("Защита нового носителя от повторной передачи после передачи, тики (защита от цепных убийств).")
                .defineInRange("transferGraceTicks", 100, 0, 12000);
        RESPAWN_GRACE_TICKS = b.comment("Защита носителя после возрождения / после смерти от моба, тики.")
                .defineInRange("respawnGraceTicks", 100, 0, 12000);
        IGNORE_CREATIVE_OBSERVERS = b.comment("Не считать наблюдателями игроков в творческом режиме.")
                .define("ignoreCreativeObservers", false);
        b.pop();

        b.comment("Реплики Cheap Trick в чате носителя").push("phrases");
        PHRASE_MIN_SECONDS = b.comment("Минимальный интервал между репликами, секунды.")
                .defineInRange("minIntervalSeconds", 45, 5, 86400);
        PHRASE_MAX_SECONDS = b.comment("Максимальный интервал между репликами, секунды.")
                .defineInRange("maxIntervalSeconds", 240, 5, 86400);
        PHRASE_MEMORY = b.comment("Сколько последних реплик не повторять.")
                .defineInRange("noRepeatMemory", 12, 0, 100);
        GLOBAL_CHAT = b.comment("Показывать реплики в общем чате (всем игрокам). По умолчанию реплики видит только носитель.")
                .define("sendToGlobalChat", false);
        GLOBAL_MESSAGE_COOLDOWN_SECONDS = b.comment("Минимальная пауза между любыми сообщениями Cheap Trick на сервере, секунды.")
                .defineInRange("globalMessageCooldownSeconds", 15, 0, 3600);
        b.pop();

        b.comment("Редкие личные сообщения случайным игрокам (с координатами носителя)").push("whispers");
        WHISPER_MIN_MINUTES = b.comment("Минимальный базовый интервал, минуты.")
                .defineInRange("minIntervalMinutes", 10, 1, 1440);
        WHISPER_MAX_MINUTES = b.comment("Максимальный базовый интервал, минуты.")
                .defineInRange("maxIntervalMinutes", 30, 1, 1440);
        WHISPER_CHANCE = b.comment("Вероятность отправки, когда истёк таймер (иначе событие переносится на ~минуту).")
                .defineInRange("chance", 0.6D, 0.0D, 1.0D);
        WHISPER_GLOBAL_COOLDOWN_SECONDS = b.comment("Глобальный кулдаун между личными сообщениями на сервере, секунды.")
                .defineInRange("globalCooldownSeconds", 300, 0, 86400);
        WHISPER_RECIPIENT_COOLDOWN_SECONDS = b.comment("Индивидуальный кулдаун для одного получателя, секунды.")
                .defineInRange("recipientCooldownSeconds", 1200, 0, 86400);
        WHISPER_MAX_RECIPIENTS = b.comment("Сколько игроков получают сообщение за одно событие.")
                .defineInRange("maxRecipientsPerEvent", 1, 1, 10);
        b.pop();

        b.comment("Враждебность мобов").push("hostility");
        HOSTILITY_ALL_PLAYERS = b.comment("true — нейтральные мобы агрессивны ко всем игрокам; false — только к носителю.")
                .define("affectAllPlayers", false);
        HOSTILITY_AFFECTS_TAMED = b.comment("Прирученные существа (волки и т.п.) тоже нападают.")
                .define("affectTamedMobs", true);
        HOSTILITY_RADIUS = b.comment("Радиус вокруг цели, в котором мобы получают агрессию, блоков.")
                .defineInRange("radius", 32, 4, 128);
        HOSTILITY_INTERVAL = b.comment("Как часто переприменять агрессию, тики.")
                .defineInRange("intervalTicks", 40, 10, 400);
        MOB_CATEGORIES = b.comment("Категории мобов, на которые распространяются проклятие (взгляд и агрессия).",
                "Допустимо: " + Arrays.toString(EntityClassification.values()))
                .defineList("supportedMobCategories",
                        Arrays.asList("MONSTER", "CREATURE", "AMBIENT", "WATER_CREATURE", "WATER_AMBIENT", "MISC"),
                        o -> o instanceof String && isCategory((String) o));
        b.pop();

        b.comment("Прочее").push("misc");
        VISUAL_MODEL = b.comment("Рисовать модель Cheap Trick на спине носителя (клиентская настройка).")
                .define("visualModel", true);
        DEBUG_LOGGING = b.comment("Подробный лог для отладки.")
                .define("debugLogging", false);
        b.pop();

        SPEC = b.build();
    }

    private CheapTrickConfig() {}

    private static boolean isCategory(String s) {
        for (EntityClassification c : EntityClassification.values()) {
            if (c.name().equals(s)) return true;
        }
        return false;
    }

    private static <T> T safe(Supplier<T> getter, T fallback) {
        try {
            return getter.get();
        } catch (IllegalStateException e) { // конфиг ещё не загружен
            return fallback;
        }
    }

    public static boolean enabled() { return safe(ENABLED::get, true); }
    public static double arrowWeight() { return safe(ARROW_WEIGHT::get, 2.0D); }
    public static boolean coexist() { return safe(COEXIST_WITH_OTHER_STANDS::get, true); }

    public static double gazeMaxDistance() { return safe(GAZE_MAX_DISTANCE::get, 24.0D); }
    public static double gazeHalfAngle() { return safe(GAZE_BACK_HALF_ANGLE::get, 60.0D); }
    public static double gazeAreaWidth() { return safe(GAZE_AREA_WIDTH::get, 0.6D); }
    public static double gazeAreaHeight() { return safe(GAZE_AREA_HEIGHT::get, 0.75D); }
    public static double playerTolerance() { return safe(GAZE_TOLERANCE::get, 0.1D); }
    public static double mobTolerance() { return safe(MOB_GAZE_TOLERANCE::get, 0.3D); }
    public static int gazeCheckInterval() { return safe(GAZE_CHECK_INTERVAL::get, 4); }
    public static int transferGraceTicks() { return safe(TRANSFER_GRACE_TICKS::get, 100); }
    public static int respawnGraceTicks() { return safe(RESPAWN_GRACE_TICKS::get, 100); }
    public static boolean ignoreCreativeObservers() { return safe(IGNORE_CREATIVE_OBSERVERS::get, false); }

    public static int phraseMinSeconds() { return Math.min(safe(PHRASE_MIN_SECONDS::get, 45), phraseMaxSeconds()); }
    public static int phraseMaxSeconds() { return Math.max(safe(PHRASE_MAX_SECONDS::get, 240), 5); }
    public static int phraseMemory() { return safe(PHRASE_MEMORY::get, 12); }
    public static boolean globalChat() { return safe(GLOBAL_CHAT::get, false); }
    public static int globalMessageCooldownSeconds() { return safe(GLOBAL_MESSAGE_COOLDOWN_SECONDS::get, 15); }

    public static int whisperMinMinutes() { return Math.min(safe(WHISPER_MIN_MINUTES::get, 10), whisperMaxMinutes()); }
    public static int whisperMaxMinutes() { return Math.max(safe(WHISPER_MAX_MINUTES::get, 30), 1); }
    public static double whisperChance() { return safe(WHISPER_CHANCE::get, 0.6D); }
    public static int whisperGlobalCooldownSeconds() { return safe(WHISPER_GLOBAL_COOLDOWN_SECONDS::get, 300); }
    public static int whisperRecipientCooldownSeconds() { return safe(WHISPER_RECIPIENT_COOLDOWN_SECONDS::get, 1200); }
    public static int whisperMaxRecipients() { return safe(WHISPER_MAX_RECIPIENTS::get, 1); }

    public static boolean hostilityAllPlayers() { return safe(HOSTILITY_ALL_PLAYERS::get, false); }
    public static boolean hostilityAffectsTamed() { return safe(HOSTILITY_AFFECTS_TAMED::get, true); }
    public static int hostilityRadius() { return safe(HOSTILITY_RADIUS::get, 32); }
    public static int hostilityInterval() { return safe(HOSTILITY_INTERVAL::get, 40); }

    private static volatile Set<EntityClassification> cachedCategories;
    private static volatile List<? extends String> cachedCategoriesSource;

    public static boolean isMobCategorySupported(EntityClassification classification) {
        List<? extends String> src = safe(MOB_CATEGORIES::get, Arrays.asList("MONSTER", "CREATURE", "AMBIENT", "WATER_CREATURE", "WATER_AMBIENT", "MISC"));
        if (src != cachedCategoriesSource) {
            Set<EntityClassification> set = new HashSet<>();
            for (String s : new ArrayList<>(src)) {
                for (EntityClassification c : EntityClassification.values()) {
                    if (c.name().equals(s)) set.add(c);
                }
            }
            cachedCategories = set;
            cachedCategoriesSource = src;
        }
        return cachedCategories.contains(classification);
    }

    public static boolean visualModel() { return safe(VISUAL_MODEL::get, true); }
    public static boolean debug() { return safe(DEBUG_LOGGING::get, false); }
}
