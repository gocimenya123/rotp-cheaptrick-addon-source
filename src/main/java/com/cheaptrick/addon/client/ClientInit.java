package com.cheaptrick.addon.client;

import java.util.Map;

import com.cheaptrick.addon.CheapTrickAddon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CheapTrickAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Map<String, PlayerRenderer> skins = Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap();
            for (String skin : new String[] { "default", "slim" }) {
                PlayerRenderer renderer = skins.get(skin);
                if (renderer != null) {
                    renderer.addLayer(new CheapTrickLayer(renderer));
                }
            }
        });
    }
}
