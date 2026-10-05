package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Full power state of the local player, sent on login, respawn, dimension change and suit change.
 * Incremental packets (energy, cooldown, progress, movement) keep it fresh afterwards.
 *
 * @param activeSuit suit id, empty string when no complete suit is worn
 * @param progress   progression of the active suit, null when no suit
 * @param energies   all energy pools of the player
 * @param cooldowns  running cooldowns
 * @param form       active symbiote form / visual state id ({@code ""} for none)
 */
public record PowerStateSnapshot(String activeSuit, ProgressSnapshot progress, List<EnergySnapshot> energies, List<CooldownSnapshot> cooldowns, String form) {

    public PowerStateSnapshot {
        energies = List.copyOf(energies);
        cooldowns = List.copyOf(cooldowns);
        form = form == null ? "" : form;
    }

    public boolean hasSuit() {
        return !this.activeSuit.isEmpty();
    }

    public void write(ByteBuf buf) {
        WireFormat.writeString(buf, this.activeSuit);
        buf.writeBoolean(this.progress != null);
        if (this.progress != null) {
            this.progress.write(buf);
        }
        WireFormat.writeVarInt(buf, this.energies.size());
        for (EnergySnapshot energy : this.energies) {
            energy.write(buf);
        }
        WireFormat.writeVarInt(buf, this.cooldowns.size());
        for (CooldownSnapshot cooldown : this.cooldowns) {
            cooldown.write(buf);
        }
        WireFormat.writeString(buf, this.form);
    }

    public static PowerStateSnapshot read(ByteBuf buf) {
        String suit = WireFormat.readString(buf);
        ProgressSnapshot progress = buf.readBoolean() ? ProgressSnapshot.read(buf) : null;
        int energyCount = WireFormat.readListSize(buf);
        List<EnergySnapshot> energies = new ArrayList<>(energyCount);
        for (int i = 0; i < energyCount; i++) {
            energies.add(EnergySnapshot.read(buf));
        }
        int cooldownCount = WireFormat.readListSize(buf);
        List<CooldownSnapshot> cooldowns = new ArrayList<>(cooldownCount);
        for (int i = 0; i < cooldownCount; i++) {
            cooldowns.add(CooldownSnapshot.read(buf));
        }
        return new PowerStateSnapshot(suit, progress, energies, cooldowns, WireFormat.readString(buf));
    }
}
