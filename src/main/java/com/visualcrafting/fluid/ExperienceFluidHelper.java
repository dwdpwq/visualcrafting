package com.visualcrafting.fluid;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public class ExperienceFluidHelper {

    /**
     * Convert milli-XP (1000 = 1 XP) to millibuckets (mB).
     * Standard: 1 XP = 20 mB, so 1000 milli-XP = 20 mB.
     * Formula: mB = milliXp / 50.
     */
    private static long toMillibuckets(long milliXp) {
        return milliXp / 50L;
    }

    /**
     * Insert overflow experience (in milli-XP) into the AE network as fluid.
     * <p>
     * Strategy:
     * 1. If network already has an experience fluid stored, use that type.
     * 2. Otherwise, use the fluid locked by ExperienceFluidFinder.
     * 3. Convert milli-XP to mB (20 mB = 1 XP).
     */
    public static boolean insertExpFluidToNetwork(IGrid grid, IActionSource source, long milliXp) {
        if (grid == null || milliXp <= 0) {
            return false;
        }
        long mB = toMillibuckets(milliXp);
        if (mB <= 0) {
            return false;
        }
        try {
            IStorageService storageService = grid.getStorageService();
            MEStorage storage = storageService.getInventory();
            if (storage == null) {
                return false;
            }

            // Step 1: check if network already has an experience fluid → use that type
            AEFluidKey networkFluid = findExistingExperienceFluid(storage);
            if (networkFluid != null) {
                long inserted = storage.insert(networkFluid, mB, Actionable.MODULATE, source);
                return inserted > 0L;
            }

            // Step 2: fall back to finder's locked fluid
            Fluid target = ExperienceFluidFinder.getTargetFluid();
            if (target == null) {
                return false;
            }
            AEFluidKey fluidKey = AEFluidKey.of(target);
            if (fluidKey == null) {
                return false;
            }
            long inserted = storage.insert(fluidKey, mB, Actionable.MODULATE, source);
            return inserted > 0L;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Scan the AE network storage for any experience fluid.
     * Returns the first AEFluidKey found that matches the forge:experience tag.
     */
    private static AEFluidKey findExistingExperienceFluid(MEStorage storage) {
        try {
            List<Fluid> sortedFluids = ExperienceFluidFinder.getSortedExperienceFluids();
            for (Fluid fluid : sortedFluids) {
                AEFluidKey fluidKey = AEFluidKey.of(fluid);
                if (fluidKey == null) continue;
                // Check if network has this fluid stored (SIMULATE extract 1 mB)
                long available = storage.extract(fluidKey, 1L, Actionable.SIMULATE, IActionSource.empty());
                if (available > 0L) {
                    return fluidKey;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
