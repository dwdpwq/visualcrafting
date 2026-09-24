package com.visualcrafting.mixin;

import com.visualcrafting.worldgen.BlockDisableRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 运行时 Block 级禁用：拦截 {@link WorldGenRegion#setBlock}，
 * 将命中禁用名单的写入替换为空气。
 * <p>选 WorldGenRegion 而非 LevelChunk：它只在世界生成期使用，天然只影响自然生成，
 * 不影响玩家/命令/脚本的后续放置。</p>
 * <p>方法签名经 1.21.1 生产 jar 实证：
 * {@code boolean setBlock(BlockPos, BlockState, int, int)}（4 参数版本，WorldGenRegion 自身重写；
 * 3 参数版本在父类 Level 中，Mixin 目标类内找不到，注入会失败）。</p>
 * <p>实现要点：{@code @ModifyVariable(argsOnly = true)} 在 HEAD 处替换入参 state——
 * 不能用 {@code @Inject} 改参数（值拷贝，原方法拿到的仍是原始 BlockState），
 * 替换后的返回值会真正成为原方法收到的 state。</p>
 */
@Mixin(WorldGenRegion.class)
public class WorldGenRegionBlockDisableMixin {

    @ModifyVariable(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState visualcrafting$disableBlock(BlockState state) {
        if (BlockDisableRegistry.isEmpty()) {
            return state;
        }
        if (BlockDisableRegistry.isDisabled(state.getBlock())) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
