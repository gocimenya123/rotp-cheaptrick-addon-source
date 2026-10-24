package com.cheaptrick.addon.logic;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.data.CheapTrickData;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Админ-команды (уровень 2): /cheaptrick info | set <игрок> | clear */
@Mod.EventBusSubscriber(modid = CheapTrickAddon.MOD_ID)
public class CheapTrickCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> d = event.getDispatcher();
        d.register(Commands.literal("cheaptrick").requires(s -> s.hasPermission(2))
                .then(Commands.literal("info").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    CheapTrickData data = CheapTrickData.get(server);
                    String text = data.hasCarrier()
                            ? "Носитель: " + data.getCarrierName() + " (" + data.getCarrier() + "), передач: " + data.getTransfers()
                            : "Носителя нет";
                    ctx.getSource().sendSuccess(new StringTextComponent(text), false);
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "player");
                    CheapTrickManager.assign(ctx.getSource().getServer(), target);
                    ctx.getSource().sendSuccess(new StringTextComponent("Cheap Trick теперь у " + target.getGameProfile().getName()), true);
                    return 1;
                })))
                .then(Commands.literal("clear").executes(ctx -> {
                    CheapTrickManager.assign(ctx.getSource().getServer(), null);
                    ctx.getSource().sendSuccess(new StringTextComponent("Носитель Cheap Trick снят"), true);
                    return 1;
                })));
    }
}
