package dev.fastchunk;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FastChunk — Chunk render speed optimization mod for Minecraft 1.21 (Fabric)
 *
 * Techniques used:
 *  1. Increase chunk builder thread count (more CPU cores used for meshing)
 *  2. Batch chunk update requests (avoid redundant rebuilds per tick)
 *  3. Prioritise chunks closest to player (render near chunks first)
 *  4. Occlusion culling: skip chunks fully hidden behind other chunks
 *  5. Reduce chunk update budget waste by capping per-tick rebuild count
 */
public class FastChunkMod implements ClientModInitializer {

    public static final String MOD_ID = "fastchunk";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ── Config ─────────────────────────────────────────────────────────────

    /** Extra worker threads for chunk mesh building. 0 = auto (cores - 1). */
    public static int extraBuilderThreads = 0;

    /** Max chunk sections rebuilt per tick. Vanilla = unlimited. */
    public static int maxRebuildsPerTick = 8;

    /** Skip rebuilding chunks that haven't changed their block data. */
    public static boolean skipUnchangedChunks = true;

    /** Prioritise rebuilding chunks near the player first. */
    public static boolean nearFirstPriority = true;

    /** Cull chunk sections with no visible faces (fully enclosed). */
    public static boolean aggressiveCulling = true;

    /** Reduce simulation distance independently of render distance. */
    public static boolean separateSimDistance = true;
    public static int simulationDistance = 5; // lower = faster ticks

    // ── Runtime state ──────────────────────────────────────────────────────

    public static int optimalThreadCount = 1;

    @Override
    public void onInitializeClient() {
        // Calculate optimal thread count based on available CPU cores
        int cores = Runtime.getRuntime().availableProcessors();
        // Use half the cores for chunk building, leave rest for game logic
        optimalThreadCount = Math.max(1, (cores / 2) + extraBuilderThreads);

        LOGGER.info("[FastChunk] Initialized!");
        LOGGER.info("[FastChunk] CPU Cores: {} | Chunk Builder Threads: {}", cores, optimalThreadCount);
        LOGGER.info("[FastChunk] Max rebuilds/tick: {} | Near-first: {} | Culling: {}",
                maxRebuildsPerTick, nearFirstPriority, aggressiveCulling);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            applySimulationDistance(client);
        });
    }

    /**
     * Sets simulation distance lower than render distance.
     * Chunks still render but mobs/redstone only tick near the player.
     */
    private void applySimulationDistance(MinecraftClient client) {
        if (!separateSimDistance) return;
        if (client.options == null) return;

        int currentSim = client.options.getSimulationDistance().getValue();
        if (currentSim > simulationDistance) {
            client.options.getSimulationDistance().setValue(simulationDistance);
            LOGGER.info("[FastChunk] Simulation distance set to {}", simulationDistance);
        }
    }
}
