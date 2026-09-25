package com.visualcrafting.mixin;

import com.visualcrafting.trade.VisualCraftingJobSiteHandler;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * VillagerProfession is a record in 1.21.1, so its job-site predicates are
 * immutable. Redirect the generated accessors instead of replacing registry
 * entries, preserving Holder/registry identity for other mods.
 */
@Mixin(VillagerProfession.class)
public abstract class VillagerProfessionJobSiteMixin {
    @Inject(method = "heldJobSite", at = @At("HEAD"), cancellable = true)
    private void visualcrafting$heldJobSite(
            CallbackInfoReturnable<Predicate<Holder<PoiType>>> cir) {
        Predicate<Holder<PoiType>> override =
                VisualCraftingJobSiteHandler.override((VillagerProfession) (Object) this);
        if (override != null) cir.setReturnValue(override);
    }

    @Inject(method = "acquirableJobSite", at = @At("HEAD"), cancellable = true)
    private void visualcrafting$acquirableJobSite(
            CallbackInfoReturnable<Predicate<Holder<PoiType>>> cir) {
        Predicate<Holder<PoiType>> override =
                VisualCraftingJobSiteHandler.override((VillagerProfession) (Object) this);
        if (override != null) cir.setReturnValue(override);
    }
}
