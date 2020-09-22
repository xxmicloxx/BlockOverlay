package com.xxmicloxx.blockoverlay.render.bridge

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.MC
import com.xxmicloxx.blockoverlay.render.Texture
import com.xxmicloxx.blockoverlay.render.Textures
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.util.math.Vector4f
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

class RectBuilder internal constructor(private val start: Vec3d, private val end: Vec3d,
                                       private val context: RenderContext) {

    private var uvStart = Vec2f(0f, 0f)
    private var uvEnd = Vec2f(1f, 1f)
    private var textureId: Identifier? = null
    private var atlasTexture: SpriteAtlasTexture? = null
    private var color: Vector4f = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    private var z = 0.0

    fun z(z: Double): RectBuilder {
        this.z = z
        return this
    }

    fun texture(tex: Identifier): RectBuilder {
        textureId = tex
        atlasTexture = null
        return this
    }

    fun uv(su: Float, sv: Float, eu: Float, ev: Float): RectBuilder {
        uvStart = Vec2f(su, sv)
        uvEnd = Vec2f(eu, ev)
        return this
    }

    fun texture(tex: Texture): RectBuilder {
        val sprite = Textures.atlas.getSprite(tex)
        uvStart = Vec2f(sprite.minU, sprite.minV)
        uvEnd = Vec2f(sprite.maxU, sprite.maxV)
        atlasTexture = sprite.atlas
        textureId = null
        return this
    }

    fun alpha(a: Float): RectBuilder {
        return color(color.x, color.y, color.z, a)
    }

    fun color(r: Float, g: Float, b: Float): RectBuilder {
        return color(r, g, b, color.w)
    }

    fun color(r: Float, g: Float, b: Float, a: Float): RectBuilder {
        color = Vector4f(r, g, b, a)
        return this
    }

    @Suppress("DuplicatedCode")
    fun draw() {
        val hasTexture = textureId != null || atlasTexture != null

        var format = VertexFormats.POSITION_COLOR_LIGHT

        if (hasTexture) {
            format = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT
            RenderSystem.enableTexture()

            if (textureId != null) {
                MC.textureManager.bindTexture(textureId)
            } else {
                atlasTexture!!.bindTexture()
            }
        }

        val alpha = color.w * context.globalAlpha

        val buf = context.tess.buffer
        buf.begin(GL11.GL_TRIANGLE_STRIP, format)

        if (hasTexture) {
            buf.vertex(start.x, z, start.z).color(color.x, color.y, color.z, alpha)
                    .texture(uvStart.x + 0.0001f, uvStart.y + 0.0001f).light(context.lightUv).next()

            buf.vertex(start.x, z, end.z).color(color.x, color.y, color.z, alpha)
                    .texture(uvStart.x + 0.0001f, uvEnd.y - 0.0001f).light(context.lightUv).next()

            buf.vertex(end.x, z, start.z).color(color.x, color.y, color.z, alpha)
                    .texture(uvEnd.x - 0.0001f, uvStart.y + 0.0001f).light(context.lightUv).next()

            buf.vertex(end.x, z, end.z).color(color.x, color.y, color.z, alpha)
                    .texture(uvEnd.x - 0.0001f, uvEnd.y - 0.0001f).light(context.lightUv).next()
        } else {
            buf.vertex(start.x, z, start.z).color(color.x, color.y, color.z, alpha).light(context.lightUv).next()
            buf.vertex(start.x, z, end.z).color(color.x, color.y, color.z, alpha).light(context.lightUv).next()
            buf.vertex(end.x, z, start.z).color(color.x, color.y, color.z, alpha).light(context.lightUv).next()
            buf.vertex(end.x, z, end.z).color(color.x, color.y, color.z, alpha).light(context.lightUv).next()
        }

        context.tess.draw()
    }
}