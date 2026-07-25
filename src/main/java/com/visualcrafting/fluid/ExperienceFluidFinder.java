package com.visualcrafting.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = "visualcrafting")
public class ExperienceFluidFinder {
    private static final ResourceLocation FALLBACK_ID =
            ResourceLocation.fromNamespaceAndPath("visualcrafting", "liquid_xp");

    private static Fluid targetFluid = null;
    private static ResourceKey<Fluid> lockedFluidKey = null;
    private static boolean usingFallback = false;

    public static Fluid getTargetFluid() {
        validateAndFind();
        return targetFluid;
    }

    /**
     * Get all known experience fluids sorted by mod jar filename (a-z).
     * The first fluid in the list is the preferred one.
     */
    public static List<Fluid> getSortedExperienceFluids() {
        List<Fluid> result = new ArrayList<>();
        Fluid fallback = BuiltInRegistries.FLUID.get(FALLBACK_ID);

        var tagOpt = BuiltInRegistries.FLUID.getTag(Tags.Fluids.EXPERIENCE);
        if (tagOpt.isEmpty()) {
            if (fallback != null) result.add(fallback);
            return result;
        }

        // Collect holders with their jar names for sorting
        record FluidEntry(Fluid fluid, String jarName) {}
        List<FluidEntry> entries = new ArrayList<>();

        tagOpt.get().forEach(holder -> {
            var keyOpt = holder.unwrapKey();
            if (keyOpt.isEmpty()) return;
            Fluid fluid = BuiltInRegistries.FLUID.get(keyOpt.get());
            if (fluid == null) return;
            if (fallback != null && fluid.isSame(fallback)) return;
            entries.add(new FluidEntry(fluid, getModJarName(holder)));
        });

        entries.sort(Comparator.comparing(e -> e.jarName, String.CASE_INSENSITIVE_ORDER));

        for (FluidEntry entry : entries) {
            result.add(entry.fluid);
        }

        if (fallback != null) {
            result.add(fallback);
        }
        return result;
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        targetFluid = null;
        lockedFluidKey = null;
        usingFallback = false;
        validateAndFind();
    }

    private static void validateAndFind() {
        if (lockedFluidKey != null) {
            Fluid fluid = BuiltInRegistries.FLUID.get(lockedFluidKey);
            if (fluid != null) {
                if (usingFallback) targetFluid = fluid;
                return;
            }
            System.err.println("[VC:ExperienceFluidFinder] Locked fluid "
                    + lockedFluidKey.location() + " disappeared, re-scanning...");
            targetFluid = null;
            lockedFluidKey = null;
            usingFallback = false;
        }

        List<Fluid> sorted = getSortedExperienceFluids();
        for (Fluid fluid : sorted) {
            Optional<ResourceKey<Fluid>> keyOpt = BuiltInRegistries.FLUID.getResourceKey(fluid);
            if (keyOpt.isPresent()) {
                lockedFluidKey = keyOpt.get();
                targetFluid = fluid;
                boolean isFallback = BuiltInRegistries.FLUID.get(FALLBACK_ID) != null
                        && fluid.isSame(BuiltInRegistries.FLUID.get(FALLBACK_ID));
                usingFallback = isFallback;
                System.err.println("[VC:ExperienceFluidFinder] Locked experience fluid: "
                        + lockedFluidKey.location() + (usingFallback ? " (fallback)" : ""));
                return;
            }
        }

        System.err.println("[VC:ExperienceFluidFinder] WARNING: no experience fluid available");
    }

    private static String getModJarName(Holder<Fluid> holder) {
        try {
            var keyOpt = holder.unwrapKey();
            if (keyOpt.isEmpty()) return "zzz_unknown";
            String namespace = keyOpt.get().location().getNamespace();
            Optional<? extends ModContainer> modOpt = ModList.get().getModContainerById(namespace);
            if (modOpt.isPresent()) {
                return modOpt.get().getModInfo().getOwningFile().getFile().getFileName();
            }
            return namespace;
        } catch (Exception e) {
            return "zzz_unknown";
        }
    }
}
