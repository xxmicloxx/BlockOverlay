package com.xxmicloxx.blockoverlay.render.bridge

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.colorVectorToInt
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.Rotation3
import net.minecraft.client.util.math.Vector4f

class TextBuilder internal constructor(private val text: String, private val context: RenderContext) {

    private var x = 0.0
    private var y = 0.0
    private var color: Vector4f = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    private var shadow = false
    private var centeredX = false
    private var centeredY = false
    private var alignLeft = false
    private var alignTop = false
    private var z = 0.0
    private var scale = 1.0f
    private var line = 0

    fun pos(x: Double, y: Double): TextBuilder {
        this.x = x
        this.y = y
        return this
    }

    fun z(z: Double): TextBuilder {
        this.z = z
        return this
    }

    fun alpha(a: Float): TextBuilder {
        return color(color.x, color.y, color.z, a)
    }

    fun color(r: Float, g: Float, b: Float): TextBuilder {
        return color(r, g, b, color.w)
    }

    fun color(r: Float, g: Float, b: Float, a: Float): TextBuilder {
        color = Vector4f(r, g, b, a)
        return this
    }

    fun shadow(): TextBuilder {
        shadow = true
        return this
    }

    fun centered(): TextBuilder {
        centeredX = true
        centeredY = true
        alignLeft = false
        alignTop = false
        return this
    }

    fun centeredX(): TextBuilder {
        centeredX = true
        alignLeft = false
        return this
    }

    fun centeredY(): TextBuilder {
        centeredY = true
        alignTop = false
        return this
    }

    fun alignLeft(): TextBuilder {
        alignLeft = true
        centeredX = false
        return this
    }

    fun alignTop(): TextBuilder {
        alignTop = true
        centeredY = false
        return this
    }

    fun scale(scale: Float): TextBuilder {
        this.scale = scale
        return this
    }

    fun line(line: Int): TextBuilder {
        this.line = line
        return this
    }

    fun draw() {
        val alpha = color.w * context.globalAlpha
        val realColor = colorVectorToInt(color, alpha)
        if ((realColor and -67108864) == 0) {
            // alpha is zero, don't render
            return
        }

        val immediate = VertexConsumerProvider.immediate(context.tess.buffer)

        val r = MinecraftClient.getInstance().textRenderer

        val defaultScale = 1f / r.fontHeight
        RenderSystem.pushMatrix()
        RenderSystem.translated(x, z, y)
        RenderSystem.scalef(defaultScale, 1.0f, defaultScale)
        RenderSystem.scalef(scale, 1.0f, scale)
        RenderSystem.rotatef(90f, 1f, 0f, 0f)

        var xOffset = 0f
        var yOffset = 0f

        if (centeredX) {
            xOffset = -(r.getStringWidth(text) / 2f)
        } else if (alignLeft) {
            xOffset = -r.getStringWidth(text).toFloat()
        }

        if (centeredY) {
            yOffset = -(r.fontHeight.toFloat() / 2f)
        } else if (alignTop) {
            yOffset = -r.fontHeight.toFloat()
        }

        yOffset += line * r.fontHeight

        r.draw(text, xOffset, yOffset, realColor, shadow, Rotation3.identity().matrix, immediate, true,
                0, context.lightUv)
        immediate.draw()

        RenderSystem.popMatrix()

        RenderSystem.disableAlphaTest()
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.depthMask(false)
    }
}