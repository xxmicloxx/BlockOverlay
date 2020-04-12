package com.xxmicloxx.blockoverlay.mixins;

import com.xxmicloxx.blockoverlay.BlockOverlayModKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void debugRendererInitTrap(MinecraftClient client, CallbackInfo ci) {
        BlockOverlayModKt.renderInit();
    }
}
