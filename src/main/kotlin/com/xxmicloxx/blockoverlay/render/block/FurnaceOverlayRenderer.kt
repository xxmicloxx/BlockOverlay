package com.xxmicloxx.blockoverlay.render.block

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import com.xxmicloxx.blockoverlay.render.state.FurnaceRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.FurnaceBlockEntity
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL11

class FurnaceOverlayRenderer : BlockOverlayRenderer {
    override val matchedBlocks: Set<BlockEntityType<out BlockEntity>>
        get() = setOf(BlockEntityType.FURNACE)

    override fun render(state: BlockRenderState<BlockEntity>, alpha: Float) {
        val furnaceState = state as FurnaceRenderState

        drawBackground(furnaceState, alpha)
    }

    private fun drawBackground(state: FurnaceRenderState, alpha: Float) {
        val tess = Tessellator.getInstance()
        val buf = tess.buffer

        /*val r = 0.05f
        val g = 0.05f
        val b = 0.05f*/

        val r = if (state.status != FurnaceRenderState.Status.DONE) 0.5f else 0.05f
        val g = if (state.status != FurnaceRenderState.Status.FAILED) 0.5f else 0.05f
        val b = 0.05f
        val a = 0.93f * alpha

        buf.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR)

        buf.vertex(0.0, 0.0, 0.0).color(r, g, b, a).next()
        buf.vertex(0.0, 0.0, 1.0).color(r, g, b, a).next()
        buf.vertex(1.0, 0.0, 0.0).color(r, g, b, a).next()
        buf.vertex(1.0, 0.0, 1.0).color(r, g, b, a).next()

        tess.draw()
    }

    override fun createRenderState(entity: BlockEntity): BlockRenderState<BlockEntity> =
        FurnaceRenderState(entity as FurnaceBlockEntity)
}