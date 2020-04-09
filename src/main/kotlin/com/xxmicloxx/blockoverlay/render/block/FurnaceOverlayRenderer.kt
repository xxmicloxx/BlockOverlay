package com.xxmicloxx.blockoverlay.render.block

import com.xxmicloxx.blockoverlay.render.MC
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import com.xxmicloxx.blockoverlay.render.state.FurnaceRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.FurnaceBlockEntity
import net.minecraft.client.render.*
import net.minecraft.util.math.Direction
import org.lwjgl.opengl.GL11

class FurnaceOverlayRenderer : BlockOverlayRenderer {
    override val matchedBlocks: Set<BlockEntityType<out BlockEntity>>
        get() = setOf(BlockEntityType.FURNACE)

    override fun render(
        state: BlockRenderState<BlockEntity>,
        side: Direction,
        alpha: Float
    ) {
        val furnaceState = state as FurnaceRenderState

        drawBackground(furnaceState, side, alpha)
    }

    private fun drawBackground(state: FurnaceRenderState, side: Direction, alpha: Float) {
        val world = MC.currentWorld ?: return
        val tess = Tessellator.getInstance()
        val buf = tess.buffer

        /*val r = 0.05f
        val g = 0.05f
        val b = 0.05f*/

        val r = if (state.status != FurnaceRenderState.Status.DONE) 0.5f else 0.05f
        val g = if (state.status != FurnaceRenderState.Status.FAILED) 0.5f else 0.05f
        val b = 0.05f
        val a = 0.93f * alpha

        MC.lightmapTextureManager.enable()

        val blockState = world.getBlockState(state.entity.pos)
        val lightCoords = WorldRenderer.getLightmapCoordinates(world, blockState, state.entity.pos.offset(side))

        buf.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR_LIGHT)

        buf.vertex(0.0, 0.0, 0.0).color(r, g, b, a).light(lightCoords).next()
        buf.vertex(0.0, 0.0, 1.0).color(r, g, b, a).light(lightCoords).next()
        buf.vertex(1.0, 0.0, 0.0).color(r, g, b, a).light(lightCoords).next()
        buf.vertex(1.0, 0.0, 1.0).color(r, g, b, a).light(lightCoords).next()

        tess.draw()

        MC.lightmapTextureManager.disable()
    }

    override fun createRenderState(entity: BlockEntity): BlockRenderState<BlockEntity> =
        FurnaceRenderState(entity as FurnaceBlockEntity)
}