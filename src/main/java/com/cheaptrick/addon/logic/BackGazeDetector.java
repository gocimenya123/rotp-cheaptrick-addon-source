package com.cheaptrick.addon.logic;

import java.util.List;

import javax.annotation.Nullable;

import com.cheaptrick.addon.config.CheapTrickConfig;
import com.github.standobyte.jojo.entity.stand.StandEntity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * Серверная проверка «кто-то видит спину носителя»: позади + взгляд в область спины + нет блоков на пути + дистанция.
 * Геометрия — в {@link BackGeometry}; здесь только сбор кандидатов и проверка препятствий.
 */
public final class BackGazeDetector {
    private BackGazeDetector() {}

    public static final class Result {
        @Nullable public final ServerPlayerEntity player;
        @Nullable public final MobEntity mob;

        private Result(@Nullable ServerPlayerEntity player, @Nullable MobEntity mob) {
            this.player = player;
            this.mob = mob;
        }
    }

    /** @return ближайший игрок-наблюдатель; если игроков нет — первый моб; иначе null. Игрок приоритетнее моба. */
    @Nullable
    public static Result detect(ServerPlayerEntity carrier) {
        if (carrier.isSleeping() || carrier.getPose() == Pose.SLEEPING) {
            return null;
        }
        World world = carrier.level;
        double maxDist = CheapTrickConfig.gazeMaxDistance();
        AxisAlignedBB search = carrier.getBoundingBox().inflate(maxDist);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, search, e -> isCandidate(e, carrier));

        ServerPlayerEntity bestPlayer = null;
        double bestDist = Double.MAX_VALUE;
        MobEntity bestMob = null;
        double bestMobDist = Double.MAX_VALUE;

        for (LivingEntity e : candidates) {
            boolean isPlayer = e instanceof ServerPlayerEntity;
            double tol = isPlayer ? CheapTrickConfig.playerTolerance() : CheapTrickConfig.mobTolerance();
            if (!seesBack(e, carrier, maxDist, tol)) {
                continue;
            }
            double d = e.distanceToSqr(carrier);
            if (isPlayer) {
                if (d < bestDist) {
                    bestDist = d;
                    bestPlayer = (ServerPlayerEntity) e;
                }
            } else if (d < bestMobDist) {
                bestMobDist = d;
                bestMob = (MobEntity) e;
            }
        }
        if (bestPlayer != null) {
            return new Result(bestPlayer, null);
        }
        return bestMob != null ? new Result(null, bestMob) : null;
    }

    private static boolean isCandidate(LivingEntity e, ServerPlayerEntity carrier) {
        if (e == carrier || !e.isAlive() || e.isSpectator() || e.isSleeping() || e instanceof StandEntity) {
            return false;
        }
        if (e.hasEffect(Effects.BLINDNESS)) {
            return false; // слепой не видит
        }
        if (e instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) e;
            return !(p.isCreative() && CheapTrickConfig.ignoreCreativeObservers());
        }
        if (e instanceof MobEntity) {
            MobEntity mob = (MobEntity) e;
            return !mob.isNoAi() && CheapTrickConfig.isMobCategorySupported(mob.getType().getCategory());
        }
        return false; // армор-стенды и пр. не наблюдатели
    }

    private static boolean seesBack(LivingEntity observer, ServerPlayerEntity carrier, double maxDist, double tolerance) {
        Vector3d eye = observer.getEyePosition(1.0F);
        Vector3d look = observer.getViewVector(1.0F);
        double[] hit = new double[3];
        boolean geometric;
        Pose pose = carrier.getPose();
        if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
            AxisAlignedBB bb = carrier.getBoundingBox();
            geometric = BackGeometry.horizontalBackHit(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ,
                    eye.x, eye.y, eye.z, look.x, look.y, look.z, maxDist, tolerance, hit);
        } else {
            geometric = BackGeometry.standingBackHit(carrier.getX(), carrier.getY(), carrier.getZ(), carrier.yBodyRot,
                    carrier.isCrouching(), eye.x, eye.y, eye.z, look.x, look.y, look.z, maxDist,
                    CheapTrickConfig.gazeHalfAngle(), CheapTrickConfig.gazeAreaWidth(), CheapTrickConfig.gazeAreaHeight(),
                    tolerance, hit);
        }
        if (!geometric) {
            return false;
        }
        // Препятствия: блоки с полной визуальной непрозрачностью между глазами и точкой на спине (стекло не мешает)
        Vector3d target = new Vector3d(hit[0], hit[1], hit[2]);
        BlockRayTraceResult obstruction = carrier.level.clip(new RayTraceContext(eye, target,
                RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.NONE, observer));
        return obstruction.getType() == RayTraceResult.Type.MISS;
    }
}
