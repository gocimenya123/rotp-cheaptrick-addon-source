package com.cheaptrick.addon.util;

import net.minecraft.util.DamageSource;

/** Урон проклятия: пробивает броню, магическую защиту, тотем бессмертия и неуязвимость (включая творческий режим). */
public class CheapTrickDamage extends DamageSource {
    public static final CheapTrickDamage INSTANCE = new CheapTrickDamage();

    private CheapTrickDamage() {
        super("cheaptrickaddon.curse");
        bypassArmor();
        bypassInvul();
        bypassMagic();
    }
}
