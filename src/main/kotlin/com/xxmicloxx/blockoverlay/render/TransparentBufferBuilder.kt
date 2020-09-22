package com.xxmicloxx.blockoverlay.render

import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.*
import java.lang.reflect.Field
import java.util.*

class TransparentBuilderImmediate :
        VertexConsumerProvider.Immediate(TransparentBufferBuilder.INSTANCE, ImmutableMap.of<RenderLayer, BufferBuilder>(
                RenderLayer.getGlint(), TransparentBufferBuilder.INSTANCES[1],
                RenderLayer.getEntityGlint(), TransparentBufferBuilder.INSTANCES[1]
        )) {

    private val layerFieldOptifine: Field?

    init {
        val cls = VertexConsumerProvider.Immediate::class.java
        layerFieldOptifine = cls.declaredFields.firstOrNull { it.type == RenderLayer::class.java }
    }

    override fun draw(layer: RenderLayer) {
        val optifineField = layerFieldOptifine

        val bufferBuilder = this.layerBuffers.getOrDefault(layer, this.fallbackBuffer) as BufferBuilder

        val bl = if (optifineField == null) {
            currentLayer == layer.asOptional()
        } else {
            val currLayer = optifineField.get(this) as RenderLayer?
            currLayer == layer
        }

        if (bl || bufferBuilder !== fallbackBuffer) {
            if (activeConsumers.remove(bufferBuilder)) {
                drawLayer(layer, bufferBuilder)
                if (bl) {
                    if (optifineField == null) {
                        currentLayer = Optional.empty()
                    } else {
                        optifineField.set(this, null)
                    }
                }
            }
        }
    }

    private fun drawLayer(layer: RenderLayer, buffer: BufferBuilder) {
        if (buffer.isBuilding) {
            buffer.sortQuads(0f, -0.5f, 1.5f)

            buffer.end()
            layer.startDrawing()
            RenderSystem.enableBlend()
            RenderSystem.disableLighting()
            BufferRenderer.draw(buffer)
            layer.endDrawing()
        }
    }
}

class TransparentBufferBuilder : BufferBuilder(2097152) {
    companion object {
        val INSTANCE: TransparentBufferBuilder
            get() = INSTANCES[0]

        var INSTANCES = arrayOf<TransparentBufferBuilder>()
            private set

        fun renderInit() {
            INSTANCES = arrayOf(TransparentBufferBuilder(), TransparentBufferBuilder())
        }
    }

    var alpha: Float = 1f
    private var hasColor = false

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        return super.color(red, green, blue, ((alpha / 255f) * this.alpha * 255f).toInt())
    }

    override fun vertex(x: Float, y: Float, z: Float, red: Float, green: Float, blue: Float, alpha: Float, u: Float, v: Float, overlay: Int, light: Int, normalX: Float, normalY: Float, normalZ: Float) {
        super.vertex(x, y, z, red, green, blue, alpha * this.alpha, u, v, overlay, light, normalX, normalY, normalZ)
    }

    override fun begin(drawMode: Int, format: VertexFormat) {
        hasColor = format.elements.contains(VertexFormats.COLOR_ELEMENT)
        if (!hasColor) {
            println("Doesn't have color")
        }
        super.begin(drawMode, format)
    }
}