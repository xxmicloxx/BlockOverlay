package com.xxmicloxx.blockoverlay.render.bridge

import com.xxmicloxx.blockoverlay.render.BarTextureRegistry
import com.xxmicloxx.blockoverlay.render.Textures
import net.minecraft.item.ItemStack
import kotlin.math.max

class ItemFrameBuilder(private val stack: ItemStack, private val context: RenderContext) {
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var height = 0.2

    fun pos(x: Double, y: Double): ItemFrameBuilder {
        this.x = x
        this.y = y
        return this
    }

    fun z(z: Double): ItemFrameBuilder {
        this.z = z
        return this
    }

    fun height(height: Double): ItemFrameBuilder {
        this.height = height
        return this
    }

    fun draw() {
        val pixel = height / 18.0

        // draw background
        context.bridge.rect(x, y, pixel * 22.0, height)
                .texture(Textures.ITEM_FRAME)
                .z(z)
                .alpha(0.9f)
                .draw()

        if (stack.isEmpty) {
            return
        }

        // draw the item
        context.bridge.item(stack)
                .pos(x + 5 * pixel, y + 1 * pixel)
                .scale(1f * pixel.toFloat() * 16f)
                .alpha(0.9f)
                .z(z)
                .draw()

        val barEntry = BarTextureRegistry.findEntry(stack.count, stack.maxCount)
        val barTexture = barEntry.texture
        val barPercentage = barEntry.getPercentage(stack.count, stack.maxCount)
        // calculate how many pixels of bar are shown
        val barPixels = max(1, (barPercentage * 16f).toInt())

        // calculate UVs
        val barSprite = Textures.atlas.getSprite(barTexture)
        val vDiff = barSprite.maxV - barSprite.minV
        val vPixel = vDiff / 16f

        // draw the bar
        context.bridge.rect(x + 1 * pixel, y + (1 + 16 - barPixels) * pixel, 3 * pixel, pixel * barPixels)
                .texture(barTexture)
                .uv(barSprite.minU, barSprite.minV + (16 - barPixels) * vPixel, barSprite.maxU, barSprite.maxV)
                .alpha(0.8f).z(z)
                .draw()

        if (stack.count != 1) {
            // draw the item count
            context.bridge.text(stack.count.toString())
                    .pos(x + 21 * pixel, y + 17 * pixel)
                    .scale(height.toFloat() / 3f)
                    .alignTop().alignLeft().z(z).draw()
        }
    }
}