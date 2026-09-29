package com.visualcrafting.mixin;

import com.visualcrafting.trade.VisualCraftingJobSiteHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Lets arbitrary (non-vanilla-POI) configured job-site blocks behave as real
 * POIs: when the vanilla POI system asks PoiTypes.forState(state) for a block
 * that is registered in world/visualcrafting/job_sites/*.json, we report the
 * mapped POI type (the block's own POI if it has one, otherwise the profession's
 * vanilla POI type). This runs on the server side only.
 *
 * Effect chain:
 *   place block -> ServerLevel.onBlockStateChange -> PoiManager.add
 *                 -> PoiTypes.forState (this mixin) -> POI record created
 *   unemployed villager -> AcquireJobSiteTask (48-block search, real pathfinding)
 *                 -> POTENTIAL_JOB_SITE -> AssignProfessionFromJobSite
 *                 -> profession matched through VillagerProfessionJobSiteMixin
 */
@Mixin(PoiTypes.class)
public abstract class PoiTypesForStateMixin {

    @Inject(method = "forState", at = @At("HEAD"), cancellable = true)
    private static void visualcrafting$forState(BlockState state,
                                                CallbackInfoReturnable<Optional<Holder<PoiType>>> cir) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ResourceKey<PoiType> key = VisualCraftingJobSiteHandler.resolveConfiguredPoiKey(server, state.getBlock());
        if (key == null) return;

        Optional<Holder<PoiType>> holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolder(key)
                .map(h -> (Holder<PoiType>) h);
        if (holder.isPresent()) {
            cir.setReturnValue(holder);
        }
    }
}
