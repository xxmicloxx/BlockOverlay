package com.xxmicloxx.blockoverlay.render.bridge

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.RenderSurface
import com.xxmicloxx.blockoverlay.render.Textures
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.WorldRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

class RenderBridge(val globalAlpha: Float, entity: BlockEntity, val side: Direction, val rotation: Direction,
                   val surfaces: List<RenderSurface>) {
    private val tess = Tessellator.getInstance()

    val lightUv: Int

    init {
        val blockState = entity.world!!.getBlockState(entity.pos)
        lightUv = WorldRenderer.getLightmapCoordinates(entity.world, blockState, entity.pos.offset(side))
    }

    private fun createRenderContext(): RenderContext =
            RenderContext(tess, lightUv, globalAlpha, this)

    fun move(z: Double) {
        RenderSystem.translated(0.0, z, 0.0)
    }

    fun rect(x: Double, y: Double, w: Double, h: Double): RectBuilder =
            RectBuilder(Vec3d(x, 0.0, y), Vec3d(x + w, 0.0, y + h), createRenderContext())

    fun text(text: String): TextBuilder =
            TextBuilder(text, createRenderContext())

    fun item(stack: ItemStack): ItemBuilder =
            ItemBuilder(stack, createRenderContext())

    fun drawBackground() {
        this.rect(0.0, 0.0, 1.0, 1.0).texture(Textures.BACKGROUND).alpha(0.7f).draw()
    }

    fun itemFrame(stack: ItemStack): ItemFrameBuilder =
            ItemFrameBuilder(stack, createRenderContext())
}