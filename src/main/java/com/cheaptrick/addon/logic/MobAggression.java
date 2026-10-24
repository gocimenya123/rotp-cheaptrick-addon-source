package com.cheaptrick.addon.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cheaptrick.addon.config.CheapTrickConfig;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.monster.HoglinEntity;
import net.minecraft.entity.monster.piglin.AbstractPiglinEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Проклятие Cheap Trick делает нейтральных мобов враждебными.
 * Никаких новых AI-целей и обработчиков на мобов не навешивается: агрессия переприменяется
 * по таймеру (HOSTILITY interval) только пока носитель онлайн; без носителя ванильное поведение не трогается.
 */
public final class MobAggression {
    private MobAggression() {}

    public static void tick(MinecraftServer server, ServerPlayerEntity carrier) {
        List<ServerPlayerEntity> targets = new ArrayList<>();
        if (CheapTrickConfig.hostilityAllPlayers()) {
            for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
                if (isValidTarget(p)) targets.add(p);
            }
        } else if (isValidTarget(carrier)) {
            targets.add(carrier);
        }
        if (targets.isEmpty()) {
            return;
        }
        Set<PlayerEntity> targetSet = targets.stream().collect(Collectors.toSet());
        double radius = CheapTrickConfig.hostilityRadius();
        boolean tamed = CheapTrickConfig.hostilityAffectsTamed();

        for (ServerPlayerEntity target : targets) {
            AxisAlignedBB box = target.getBoundingBox().inflate(radius);
            for (MobEntity mob : target.level.getEntitiesOfClass(MobEntity.class, box,
                    m -> m.isAlive() && !m.isNoAi() && canBeAggressive(m, tamed))) {
                apply(mob, target, targetSet);
            }
        }
    }

    private static boolean isValidTarget(ServerPlayerEntity p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }


    private static boolean canBeAggressive(MobEntity mob, boolean tamedAllowed) {
        if (!CheapTrickConfig.isMobCategorySupported(mob.getType().getCategory())) {
            return false;
        }
        if (!tamedAllowed && mob instanceof TameableEntity && ((TameableEntity) mob).isTame()) {
            return false;
        }

        // Только существа, способные атаковать
        return mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                || mob instanceof IAngerable;
    }


    private static void apply(MobEntity mob, ServerPlayerEntity target, Set<PlayerEntity> targetSet) {
        if (mob.getTarget() != null && targetSet.contains(mob.getTarget()) && mob.getTarget().isAlive()) {
            // Уже воюет с целью проклятия — переприменяем только «тихую» злость
            refreshAnger(mob, target);
            return;
        }
        if (mob instanceof TameableEntity) {
            TameableEntity pet = (TameableEntity) mob;
            if (pet.isInSittingPose() || pet.isOrderedToSit()) {
                pet.setOrderedToSit(false);
                pet.setInSittingPose(false);
            }
        }
        refreshAnger(mob, target);

        // Существа на Brain API (пиглины, хоглины): золотая броня и «нейтральность» игнорируются
        if (mob instanceof AbstractPiglinEntity || mob instanceof HoglinEntity) {
            Brain<?> brain = mob.getBrain();
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
            brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
        }
        // Универсально: обычные цели AI (големы, пчёлы, волки, белые медведи, эндермены, пауки, снеговики…)
        mob.setTarget(target);
    }

    private static void refreshAnger(MobEntity mob, ServerPlayerEntity target) {
        if (mob instanceof IAngerable) {
            IAngerable angerable = (IAngerable) mob;
            angerable.setPersistentAngerTarget(target.getUUID());
            angerable.startPersistentAngerTimer();
        }
    }
}
