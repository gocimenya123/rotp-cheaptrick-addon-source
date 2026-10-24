package com.cheaptrick.addon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cheaptrick.addon.config.CheapTrickConfig;
import com.cheaptrick.addon.init.InitStands;
import com.cheaptrick.addon.net.CheapTrickNetwork;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CheapTrickAddon.MOD_ID)
public class CheapTrickAddon {
    public static final String MOD_ID = "cheaptrickaddon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public CheapTrickAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        InitStands.STANDS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CheapTrickConfig.SPEC, "cheaptrickaddon-common.toml");
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CheapTrickNetwork::register);
    }

    public static void debug(String msg, Object... args) {
        if (CheapTrickConfig.debug()) {
            LOGGER.info("[debug] " + msg, args);
        }
    }
}
