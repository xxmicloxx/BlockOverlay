package com.xxmicloxx.blockoverlay.render.block

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.Textures
import com.xxmicloxx.blockoverlay.render.bridge.RenderBridge
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import com.xxmicloxx.blockoverlay.render.state.ChestRenderInfo
import com.xxmicloxx.blockoverlay.render.state.ChestRenderState
import com.xxmicloxx.blockoverlay.render.state.ContainerRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import org.lwjgl.opengl.GL11

class ChestOverlayRenderer : ContainerOverlayRenderer() {
    override val matchedBlocks: Set<BlockEntityType<out BlockEntity>>
        get() = setOf(BlockEntityType.CHEST)

    override fun getContainerStatus(state: ContainerRenderState<BlockEntity>): ContainerRenderState.Status =
            super.getContainerStatus((state as ChestRenderState).primaryRenderState)

    override fun startRender(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge) {
        val selfState = (state as ChestRenderState)
        val primaryState = selfState.primaryRenderState

        // we are a chest, translate less
        bridge.move(0.005)

        val info = selfState.getRenderInfo(bridge.side, bridge.rotation, bridge.surfaces, bridge.lightUv)
        val types = primaryState.inventory
                .filter { it.item != Items.AIR }
                .groupBy { it.item }
                .keys.size

        if (primaryState.status != ContainerRenderState.Status.LOADED) {
            drawChestBackground(info, bridge)
            return
        }

        val otherState = selfState.otherRenderState
        if (!info.drawInterleaved || types > 1 || otherState == null) {
            // draw normal
            drawChestBackground(info, bridge)
            return
        }

        if (info.drawSecond) {
            // do not draw anything
            return
        }

        // draw background twice
        drawChestBackground(info, bridge)

        RenderSystem.pushMatrix()
        RenderSystem.translatef(1f, 0f, 0f)
        val otherInfo =
                otherState.getRenderInfo(bridge.side, bridge.rotation, bridge.surfaces, bridge.lightUv)

        drawChestBackground(otherInfo, bridge)
        RenderSystem.popMatrix()
    }

    private fun drawChestBackground(info: ChestRenderInfo, bridge: RenderBridge) {
        if (!info.hasSecond) {
            // normal draw
            bridge.drawBackground()
            return
        }

        RenderSystem.pushMatrix()
        RenderSystem.translated(0.5, 0.0, 0.5)

        if (!info.drawInterleaved) {
            // rotate by 90deg
            RenderSystem.rotatef(-90f, 0f, 1f, 0f)
        }

        if (info.drawSecond) {
            // mirror
            GL11.glCullFace(GL11.GL_FRONT)
            RenderSystem.scalef(-1f, 1f, 1f)
        }

        bridge.rect(-0.5, -0.5, 1.0, 1.0).texture(Textures.BACKGROUND_DOUBLE).alpha(0.7f).draw()

        if (info.drawSecond) {
            GL11.glCullFace(GL11.GL_BACK)
        }

        RenderSystem.popMatrix()
    }

    override fun drawContent(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge) {
        val selfChestState = state as ChestRenderState
        val chestState = selfChestState.primaryRenderState

        val renderInfo =
                selfChestState.getRenderInfo(bridge.side, bridge.rotation, bridge.surfaces, bridge.lightUv)

        val cache = chestState.getCache(renderInfo)

       if (bridge.globalAlpha != 1f) {
            // do not use cache
            redrawContent(chestState, bridge, renderInfo)
            return
        }

        if (chestState.pollUpdate()) {
            chestState.clearCache()
        }

        if (!cache.isCached) {
            cache.startDrawing(true)
            redrawContent(chestState, bridge, renderInfo)
            cache.finishDrawing()
        } else {
            cache.draw()
        }
    }

    private fun redrawContent(chestState: ChestRenderState, bridge: RenderBridge, info: ChestRenderInfo) {
        val accumulatedItems = chestState.inventory
                .filter { it.item != Items.AIR }
                .groupingBy { it.item }
                .aggregate { _, accumulator: Int?, element, first ->
                    if (first) {
                        element.count
                    } else {
                        accumulator!! + element.count
                    }
                }
                .map {
                    @Suppress("USELESS_ELVIS") // circumvent compiler bug
                    ItemStack(it.key, it.value ?: 0)
                }
                .sortedByDescending { it.count }


        drawGrid(accumulatedItems, bridge, info)
    }

    private fun drawGrid(items: List<ItemStack>, bridge: RenderBridge, info: ChestRenderInfo) {
        val rows: Int
        val cols: Int
        val height: Double
        var startX: Double
        var startY: Double
        var endX: Double
        var endY: Double

        val itemCount = if (!info.hasSecond) items.size else (items.size + 1) / 2
        when {
            items.size <= 1 && info.drawInterleaved -> {
                if (info.drawSecond) {
                    // let chest 1 draw
                    return
                }

                // center on side
                rows = 1
                cols = 1
                height = 0.6
                startY = 0.5 - height / 2
                startX = 1 - height / 18 * 22 / 2
                // ignore end
                endX = 0.0
                endY = 0.0
            }
            itemCount <= 1 -> {
                rows = 1
                cols = 1
                height = 0.5
                startY = 0.5 - height / 2
                startX = 0.5 - height / 18 * 22 / 2
                // ignore end
                endX = 0.0
                endY = 0.0
            }
            itemCount in 2..4 -> {
                rows = 2
                cols = 2
                height = 0.3
                startX = 0.1
                startY = 0.15
                endX = 0.9 - height / 18 * 22
                endY = 0.85 - height

                if (info.drawInterleaved) {
                    if (!info.drawSecond) {
                        endX = 0.95 - height / 18 * 22
                    } else {
                        startX = 0.05
                    }
                } else if (info.hasSecond) {
                    if (!info.drawSecond) {
                        endY = 0.95 - height
                    } else {
                        startY = 0.05
                    }
                }
            }
            else -> {
                rows = 3
                cols = 3
                height = 0.2
                startX = 0.1
                startY = 0.15
                endX = 0.9 - height / 18 * 22
                endY = 0.85 - height

                if (info.drawInterleaved) {
                    if (!info.drawSecond) {
                        endX = 0.96 - height / 18 * 22
                    } else {
                        startX = 0.04
                    }
                } else if (info.hasSecond) {
                    if (!info.drawSecond) {
                        endY = 0.96 - height
                    } else {
                        startY = 0.04
                    }
                }
            }
        }

        val stepX = if (cols > 1) (endX - startX) / (cols - 1) else 0.0
        val stepY = if (rows > 1) (endY - startY) / (rows - 1) else 0.0

        var idx = 0
        if (info.drawSecond) {
            idx = if (info.drawInterleaved) {
                cols
            } else {
                cols * rows
            }
        }

        for (r in (0 until rows)) {
            val y = startY + r * stepY
            for (c in (0 until cols)) {
                val x = startX + c * stepX
                bridge.itemFrame(items.getOrElse(idx++) { ItemStack.EMPTY })
                        .pos(x, y).z(0.01).height(height).draw()
            }

            if (info.drawInterleaved) {
                idx += cols
            }
        }
    }

    override fun createRenderState(entity: BlockEntity): BlockRenderState<BlockEntity> =
            ChestRenderState(entity as ChestBlockEntity)
}