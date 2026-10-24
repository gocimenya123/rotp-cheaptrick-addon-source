package com.cheaptrick.addon.data;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Постоянное серверное хранилище (world/data/cheaptrickaddon_carrier.dat): единственный текущий носитель.
 * Пережидает перезапуск сервера, смерть, перезаход. Если аддон удалить, файл просто игнорируется.
 */
public class CheapTrickData extends WorldSavedData {
    public static final String NAME = "cheaptrickaddon_carrier";

    @Nullable
    private UUID carrier;
    private String carrierName = "";
    private long transfers;

    public CheapTrickData() {
        super(NAME);
    }

    public static CheapTrickData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CheapTrickData::new, NAME);
    }

    @Nullable
    public UUID getCarrier() {
        return carrier;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public boolean hasCarrier() {
        return carrier != null;
    }

    public boolean isCarrier(UUID id) {
        return carrier != null && carrier.equals(id);
    }

    public long getTransfers() {
        return transfers;
    }

    public void setCarrier(@Nullable UUID id, String name, boolean countTransfer) {
        this.carrier = id;
        this.carrierName = id == null ? "" : name;
        if (countTransfer) {
            transfers++;
        }
        setDirty();
    }

    @Override
    public void load(CompoundNBT nbt) {
        carrier = nbt.hasUUID("Carrier") ? nbt.getUUID("Carrier") : null;
        carrierName = nbt.getString("CarrierName");
        transfers = nbt.getLong("Transfers");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        if (carrier != null) {
            nbt.putUUID("Carrier", carrier);
        }
        nbt.putString("CarrierName", carrierName);
        nbt.putLong("Transfers", transfers);
        return nbt;
    }
}
