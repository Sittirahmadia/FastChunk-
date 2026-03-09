package dev.fastchunk.mixin;

import dev.fastchunk.FastChunkMod;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;

/**
 * ClientChunkManagerMixin
 *
 * MC 1.21: unloadChunk() and loadChunkFromPacket() method names changed.
 * We now only hook into getChunk() to throttle excessive chunk lookups,
 * which is stable across versions.
 *
 * Chunk load throttling is handled via tick events in FastChunkMod instead.
 */
@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {

    private static int chunkLookupCount = 0;

    /**
     * Count chunk lookups per tick for diagnostics.
     * This is a read-only hook — safe across MC versions.
     */
    @Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            at = @At("HEAD"))
    private void onGetChunk(int x, int z, net.minecraft.world.chunk.ChunkStatus status,
                            boolean create,
                            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.chunk.Chunk> cir) {
        chunkLookupCount++;
    }

    private static int getAndResetLookupCount() {
        int c = chunkLookupCount;
        chunkLookupCount = 0;
        return c;
    }
}
