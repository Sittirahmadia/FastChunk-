package dev.fastchunk.mixin;

import dev.fastchunk.FastChunkMod;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * ChunkBuilderMixin
 *
 * Overrides the number of worker threads used by ChunkBuilder.
 *
 * Vanilla Minecraft uses very few threads for chunk mesh building.
 * By increasing thread count based on available CPU cores,
 * chunks are triangulated and uploaded to GPU much faster,
 * reducing the "popping" effect when loading new areas.
 */
@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {

    /**
     * Intercept the thread count passed to the ChunkBuilder constructor
     * and replace it with our optimized count.
     *
     * Target: the int argument that sets the worker thread pool size.
     */
    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/Executors;newFixedThreadPool(ILjava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"
        ),
        index = 0
    )
    private int modifyThreadCount(int original) {
        int optimized = FastChunkMod.optimalThreadCount;
        FastChunkMod.LOGGER.info("[FastChunk] ChunkBuilder threads: {} → {}", original, optimized);
        return optimized;
    }
}
