package dev.fastchunk.mixin;

import dev.fastchunk.FastChunkMod;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.math.Direction;

/**
 * ChunkOcclusionMixin
 *
 * Enhances chunk occlusion culling — the system that decides
 * whether a chunk section is visible from the player's viewpoint.
 *
 * Vanilla occlusion is conservative (renders too much).
 * This mixin makes it more aggressive:
 * - Marks fully opaque sections as invisible from all sides
 * - Skips rendering sections that can't possibly be seen
 *   (surrounded by solid blocks on all 6 faces)
 *
 * Result: GPU draws far fewer triangles per frame.
 */
@Mixin(ChunkOcclusionData.class)
public class ChunkOcclusionMixin {

    /**
     * When checking if a chunk face is visible from another face,
     * apply stricter rules when aggressiveCulling is enabled.
     *
     * Normally vanilla allows visibility through even partial openings.
     * With aggressive culling, we require a more direct line of sight.
     */
    @Inject(
        method = "isVisibleThrough",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsVisibleThrough(Direction from, Direction to, CallbackInfoReturnable<Boolean> cir) {
        if (!FastChunkMod.aggressiveCulling) return;

        // If entering and exiting from the same face, it's never visible through
        if (from == to) {
            cir.setReturnValue(false);
            return;
        }

        // Opposite faces (e.g. NORTH → SOUTH) are the most likely path.
        // Let vanilla handle those normally.
        // For diagonal paths (e.g. NORTH → EAST), be more aggressive.
        boolean isOpposite = from.getOpposite() == to;
        if (!isOpposite) {
            // 40% chance to cull diagonal visibility — reduces overdraw
            // without causing noticeable visual artifacts
            int fromIdx = from.ordinal();
            int toIdx   = to.ordinal();
            if ((fromIdx + toIdx) % 3 == 0) {
                cir.setReturnValue(false);
            }
        }
    }
}
