package com.cheaptrick.addon.stand;

import com.cheaptrick.addon.config.CheapTrickConfig;
import com.cheaptrick.addon.data.CheapTrickData;
import com.cheaptrick.addon.logic.CheapTrickManager;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.NoSummonStandType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * Тип стенда Cheap Trick. Живёт в реестре RotP, поэтому его можно получить стрелой.
 * Источник истины о носителе — {@link CheapTrickData}; сам StandPower лишь «зеркало» для отображения в интерфейсе RotP.
 */
public class CheapTrickStandType extends NoSummonStandType<StandStats> {
    private StandStats cachedStats;
    private double cachedWeight = Double.NaN;
    private StandStats zeroWeightStats;

    public CheapTrickStandType(NoSummonStandType.Builder<StandStats> builder) {
        super(builder);
    }

    public static StandStats.Builder statsBuilder(double weight) {
        return new StandStats.Builder().power(0).speed(0).range(1).durability(0).precision(0).randomWeight(weight);
    }

    /**
     * Шанс выпадения из стрелы = вес в случайном выборе RotP (StandUtil.randomStandOrError).
     * Вес берётся из конфига; он обнуляется, если аддон выключен или носитель уже существует,
     * — тогда RotP не включает Cheap Trick в пул (PLAYER_ARROW требует вес > 0).
     */
    @Override
    public StandStats getStats() {
        double weight = CheapTrickConfig.enabled() ? CheapTrickConfig.arrowWeight() : 0.0D;
        if (weight > 0 && carrierExistsOnServer()) {
            weight = 0.0D;
        }
        if (weight <= 0) {
            if (zeroWeightStats == null) {
                zeroWeightStats = statsBuilder(0.0D).build();
            }
            return zeroWeightStats;
        }
        if (cachedStats == null || cachedWeight != weight) {
            cachedStats = statsBuilder(weight).build();
            cachedWeight = weight;
        }
        return cachedStats;
    }

    private static boolean carrierExistsOnServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && CheapTrickData.get(server).hasCarrier();
    }

    /** Носитель сохраняет стенд после смерти — Cheap Trick остаётся проклятием игрока. */
    @Override
    public boolean keepOnDeath(IStandPower power) {
        LivingEntity user = power.getUser();
        if (user != null && CheapTrickManager.isCarrier(user)) {
            return true;
        }
        return super.keepOnDeath(power);
    }
}
