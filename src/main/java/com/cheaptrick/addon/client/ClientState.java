package com.cheaptrick.addon.client;

import java.util.UUID;

import javax.annotation.Nullable;

/** Клиентское отражение серверного состояния (только для отрисовки; логики не содержит). */
public final class ClientState {
    @Nullable
    private static volatile UUID carrier;

    private ClientState() {}

    public static void setCarrier(@Nullable UUID id) {
        carrier = id;
    }

    public static boolean isCarrier(UUID id) {
        UUID c = carrier;
        return c != null && c.equals(id);
    }
}
