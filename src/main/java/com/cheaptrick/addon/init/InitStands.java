package com.cheaptrick.addon.init;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.stand.CheapTrickStandType;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.NoSummonStandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType.StandSurvivalGameplayPool;
import com.github.standobyte.jojo.util.mod.StoryPart;

import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class InitStands {
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<StandType<?>> STANDS = DeferredRegister.create(
            (Class<StandType<?>>) ((Class<?>) StandType.class), CheapTrickAddon.MOD_ID);

    /**
     * Cheap Trick — стенд без призываемой сущности (как Mr. President в самом RotP):
     * его визуальная модель рисуется слоем рендера прямо на спине носителя.
     * Остаётся в обычном пуле стрелы (PLAYER_ARROW), вес задаётся конфигом (см. CheapTrickStandType#getStats).
     */
    public static final RegistryObject<StandType<StandStats>> CHEAP_TRICK = STANDS.register("cheap_trick",
            () -> new CheapTrickStandType(new NoSummonStandType.Builder<StandStats>()
                    .color(0x6E5A7A)
                    .storyPartName(StoryPart.DIAMOND_IS_UNBREAKABLE.getName())
                    .defaultStats(StandStats.class, CheapTrickStandType.statsBuilder(2.0D))
                    .setSurvivalGameplayPool(StandSurvivalGameplayPool.PLAYER_ARROW)));
}
