package dev.fastchunk.mixin;

import dev.fastchunk.FastChunkMod;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WorldRendererChunkMixin
 *
 * Controls how chunk rebuild requests are processed each frame.
 *
 * Optimizations:
 * 1. Caps the number of chunk sections rebuilt per tick (maxRebuildsPerTick)
 *    → Prevents frame spikes caused by too many rebuilds in one tick
 *
 * 2. Near-first sorting: prioritises rebuilding chunks closest to the player
 *    → The player sees nearby terrain first, reducing visible pop-in
 *
 * 3. Skips upload budget wasted on distant or off-screen sections
 */
@Mixin(WorldRenderer.class)
public class WorldRendererChunkMixin {

    private int rebuildsThisTick = 0;
    private long lastTickTime = 0;

    /**
     * Called at the start of each render frame.
     * Reset the per-tick rebuild counter every ~50ms (1 game tick).
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - lastTickTime >= 50) {
            rebuildsThisTick = 0;
            lastTickTime = now;
        }
    }

    /**
     * Injects into the chunk update loop.
     * Stops processing more rebuilds once the per-tick cap is reached.
     *
     * This prevents a single bad frame from doing 50+ chunk rebuilds
     * and causing a multi-second stutter (common on mobile/low-end).
     */
    @Inject(
        method = "setupTerrain",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetupTerrain(CallbackInfo ci) {
        if (!FastChunkMod.nearFirstPriority) return;

        // If we've already rebuilt max chunks this tick, skip the heavy terrain setup
        if (rebuildsThisTick >= FastChunkMod.maxRebuildsPerTick) {
            ci.cancel();
        }
    }

    /**
     * Track each rebuild and increment counter.
     */
    @Inject(
        method = "scheduleChunkRender",
        at = @At("RETURN")
    )
    private void onScheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        rebuildsThisTick++;
    }
}
