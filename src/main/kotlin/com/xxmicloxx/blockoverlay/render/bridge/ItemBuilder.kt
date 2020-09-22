package com.xxmicloxx.blockoverlay.render.bridge

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.MC
import com.xxmicloxx.blockoverlay.render.TransparentBufferBuilder
import com.xxmicloxx.blockoverlay.render.TransparentBuilderImmediate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack

class ItemBuilder internal constructor(private val stack: ItemStack, private val context: RenderContext) {
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var alpha = 1f
    private var centeredX = false
    private var centeredY = false
    private var alignLeft = false
    private var alignTop = false
    private var scale = 1f

    fun pos(x: Double, y: Double): ItemBuilder {
        this.x = x
        this.y = y
        return this
    }

    fun z(z: Double): ItemBuilder {
        this.z = z
        return this
    }

    fun alpha(alpha: Float): ItemBuilder {
        this.alpha = alpha
        return this
    }

    fun centered(): ItemBuilder {
        centeredX = true
        centeredY = true
        alignLeft = false
        alignTop = false
        return this
    }

    fun centeredX(): ItemBuilder {
        centeredX = true
        alignLeft = false
        return this
    }

    fun centeredY(): ItemBuilder {
        centeredY = true
        alignTop = false
        return this
    }

    fun alignLeft(): ItemBuilder {
        alignLeft = true
        centeredX = false
        return this
    }

    fun alignTop(): ItemBuilder {
        alignTop = true
        centeredY = false
        return this
    }

    fun scale(scale: Float): ItemBuilder {
        this.scale = scale
        return this
    }

    fun draw() {
        val renderer = MinecraftClient.getInstance().itemRenderer

        RenderSystem.pushMatrix()
        RenderSystem.translated(x, z, y)
        RenderSystem.scalef(1f, 0.001f, 1f)
        RenderSystem.scalef(scale, 1f, scale)

        var offsetX = 0.5f
        var offsetY = 0.5f

        if (centeredX) {
            offsetX = 0f
        } else if (alignLeft) {
            offsetX = -0.5f
        }

        if (centeredY) {
            offsetY = 0f
        } else if (alignTop) {
            offsetY = -0.5f
        }

        RenderSystem.translatef(offsetX, 0.5f, offsetY)

        RenderSystem.rotatef(90f, 1f, 0f, 0f)
        RenderSystem.rotatef(180f, 1f, 0f, 0f)

        renderer.zOffset = -100f

        //MC.textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX)
        //MC.textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX)!!.setFilter(false, false)
        MC.lightmapTextureManager.enable()
        RenderSystem.enableRescaleNormal()
        RenderSystem.enableAlphaTest()
        RenderSystem.defaultAlphaFunc()
        RenderSystem.enableCull()
        RenderSystem.disableLighting()

        val model = renderer.getHeldItemModel(stack, MC.currentWorld, MC.player)
        val matrix = MatrixStack()
        val immediate = TransparentBuilderImmediate()

        val resultAlpha = alpha * context.globalAlpha
        TransparentBufferBuilder.INSTANCE.alpha = resultAlpha

        renderer.renderItem(stack, ModelTransformation.Mode.GUI, false, matrix, immediate, context.lightUv,
                OverlayTexture.DEFAULT_UV, model)

        immediate.draw()

        RenderSystem.popMatrix()
        RenderSystem.disableAlphaTest()
        RenderSystem.disableRescaleNormal()
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.enableBlend()
        RenderSystem.disableLighting()
    }
}