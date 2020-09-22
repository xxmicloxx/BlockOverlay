package com.xxmicloxx.blockoverlay.mixins;

import com.xxmicloxx.blockoverlay.providers.FontStorageProvider;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin implements AutoCloseable, FontStorageProvider {

    @Shadow
    private FontStorage fontStorage;

    public FontStorage blockoverlay_getFontStorage() {
        return fontStorage;
    }
}
