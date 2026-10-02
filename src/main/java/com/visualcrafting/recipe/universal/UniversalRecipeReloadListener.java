package com.visualcrafting.recipe.universal;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Rebuilds the universal recipe index whenever datapacks/resources reload.
 */
public final class UniversalRecipeReloadListener implements PreparableReloadListener {
    @Override
    public String getName() {
        return "VisualCrafting universal recipe index";
    }

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier stage,
            ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor) {
        return CompletableFuture
                .supplyAsync(() -> UniversalRecipeScanner.scan(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait)
                .thenAcceptAsync(UniversalRecipeIndex::replace, gameExecutor);
    }
}
