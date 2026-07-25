package com.visualcrafting.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;

public class ChemSlotData {
    public final String chemicalType;
    public final String chemicalId;
    public final String chemicalName;
    public final int tintColor;
    public final boolean isTagMode;
    public final String tagId;

    public ChemSlotData(String chemicalType, String chemicalId, String chemicalName, int tintColor) {
        this(chemicalType, chemicalId, chemicalName, tintColor, false, null);
    }

    public ChemSlotData(String chemicalType, String chemicalId, String chemicalName, int tintColor, boolean isTagMode, String tagId) {
        this.chemicalType = chemicalType;
        this.chemicalId = chemicalId;
        this.chemicalName = chemicalName;
        this.tintColor = tintColor;
        this.isTagMode = isTagMode;
        this.tagId = tagId;
    }

    public static ChemSlotData forTag(String tagLoc) {
        String id = tagLoc.contains(":") ? tagLoc.substring(tagLoc.lastIndexOf(':') + 1) : tagLoc;
        return new ChemSlotData("TAG", tagLoc, id, 0xFF0000, true, tagLoc);
    }

    public static void writeToBuf(RegistryFriendlyByteBuf buf, ChemSlotData data) {
        if (data == null) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeUtf(data.chemicalType);
        buf.writeUtf(data.chemicalId);
        buf.writeUtf(data.chemicalName != null ? data.chemicalName : "");
        buf.writeVarInt(data.tintColor);
        buf.writeBoolean(data.isTagMode);
        buf.writeUtf(data.tagId != null ? data.tagId : "");
    }

    public static ChemSlotData readFromBuf(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) return null;
        String chemicalType = buf.readUtf();
        String chemicalId = buf.readUtf();
        String chemicalName = buf.readUtf();
        int tintColor = buf.readVarInt();
        boolean isTagMode = buf.readBoolean();
        String tagId = buf.readUtf();
        return new ChemSlotData(chemicalType, chemicalId, chemicalName, tintColor, isTagMode, tagId);
    }
}
