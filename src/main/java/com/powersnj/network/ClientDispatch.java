package com.powersnj.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Runs client-only packet handling. The runnable is created inside a supplier that is only invoked
 * on the physical client, so client classes are never loaded on a dedicated server.
 */
final class ClientDispatch {

    private ClientDispatch() {
    }

    @SuppressWarnings("deprecation")
    static void run(java.util.function.Supplier<Runnable> runnable) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, runnable);
    }
}
