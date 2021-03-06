package com.xxmicloxx.blockoverlay.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xxmicloxx.blockoverlay.BlockOverlayModKt;
import com.xxmicloxx.blockoverlay.render.MC;
import com.xxmicloxx.blockoverlay.render.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mark Vainomaa
 */
@Mixin(value = WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/client/util/math/" +
                    "MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/" +
                    "render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;F)V",
            shift = At.Shift.BEFORE
    ))
    private void beforeRenderParticles(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                                     Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                                     Matrix4f matrix4f, CallbackInfo ci) {

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrices.peek().getModel());

        BlockOverlayModKt.render();

        RenderSystem.popMatrix();
    }
}