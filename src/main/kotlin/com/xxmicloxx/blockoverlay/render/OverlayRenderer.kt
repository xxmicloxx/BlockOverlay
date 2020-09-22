package com.xxmicloxx.blockoverlay.render

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.ContainerHelper
import com.xxmicloxx.blockoverlay.render.block.BlockOverlayRenderer
import com.xxmicloxx.blockoverlay.render.block.ChestOverlayRenderer
import com.xxmicloxx.blockoverlay.render.block.FurnaceOverlayRenderer
import com.xxmicloxx.blockoverlay.render.bridge.RenderBridge
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector4f
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RenderSurface(val state: BlockRenderState<BlockEntity>, val side: Direction, val distanceSq: Float)

object OverlayRenderer {
    private const val CACHE_DISTANCE = 32.0
    private const val CACHE_DISTANCE_SQUARED = CACHE_DISTANCE * CACHE_DISTANCE

    private const val RENDER_DISTANCE = 16.0
    private const val RENDER_DISTANCE_SQUARED = RENDER_DISTANCE * RENDER_DISTANCE

    private const val FADING_DISTANCE = 12.0
    private const val FADING_DISTANCE_SQUARED = FADING_DISTANCE * FADING_DISTANCE

    private const val OPACITY_FADE_RATE = 2.0f

    enum class State(val enabled: Boolean) {
        STARTING(true),
        ENABLED(true),
        STOPPING(false),
        DISABLED(false)
    }

    private val rendererRegistry = mutableMapOf<BlockEntityType<out BlockEntity>, BlockOverlayRenderer>()
    private var renderStates = mutableListOf<BlockRenderState<BlockEntity>>()
    private var lastWorld: World? = null

    var currentState = State.DISABLED
        private set

    private var enableOpacity = 0.0f

    private var currentRotation: Direction? = null


    fun enable() {
        if (currentState != State.ENABLED && currentState != State.STARTING) {
            currentState = State.STARTING
            ContainerHelper.resume()
        }
    }

    fun disable() {
        if (currentState != State.DISABLED && currentState != State.STOPPING) {
            currentState = State.STOPPING
            ContainerHelper.pause()
        }
    }

    init {
        registerRenderer(FurnaceOverlayRenderer())
        registerRenderer(ChestOverlayRenderer())
    }

    fun getRenderState(pos: BlockPos): BlockRenderState<BlockEntity>? =
            renderStates.find { it.entity.pos == pos }

    private fun registerRenderer(renderer: BlockOverlayRenderer) {
        renderer.matchedBlocks.forEach {
            rendererRegistry.putIfAbsent(it, renderer)
        }
    }

    private fun destroyState() {
        val oldStates = renderStates
        renderStates = mutableListOf()
        oldStates.forEach {
            it.destroy()
        }
    }

    private fun cameraDistanceSq(pos: BlockPos): Double {
        val cam = MC.camera
        val vec = cam.pos.subtract(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        return vec.lengthSquared()
    }

    fun tick() {
        val world = MC.currentWorld
        if (lastWorld != world) {
            destroyState()
            lastWorld = world
        }

        world ?: return

        if (currentState == State.DISABLED) {
            destroyState()
            return
        }

        val entities = world.blockEntities
            .filter { rendererRegistry.containsKey(it.type) }
            .filter { cameraDistanceSq(it.pos) <= CACHE_DISTANCE_SQUARED }
            .toHashSet()

        // destroy states which are not in entity list
        renderStates
            .filter { !entities.contains(it.entity) }
            .forEach {
                renderStates.remove(it)
                it.destroy()
            }

        if (currentState != State.STOPPING) {
            // add states that should be displayed
            entities
                .filter { cameraDistanceSq(it.pos) <= RENDER_DISTANCE_SQUARED }
                .filter { matchEntity -> renderStates.none { it.entity == matchEntity } }
                .forEach {
                    val renderer = rendererRegistry[it.type] ?: return@forEach
                    renderStates.add(renderer.createRenderState(it))
                }
        }
    }

    private fun startRender() {
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(false)

        RenderSystem.shadeModel(GL11.GL_SMOOTH)

        RenderSystem.disableAlphaTest()
        RenderSystem.defaultAlphaFunc()

        RenderSystem.disableTexture()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        RenderSystem.enableCull()

        MC.lightmapTextureManager.enable()
    }

    private fun endRender() {
        MC.lightmapTextureManager.disable()
        RenderSystem.enableAlphaTest()
        //RenderSystem.enableBlend()
        RenderSystem.enableTexture()
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(true)
        RenderSystem.shadeModel(GL11.GL_FLAT)
    }

    fun render(delta: Float) {
        val world = MC.currentWorld
        if (world == null || world != lastWorld) {
            // wait for tick and loaded world
            return
        }

        val player = MC.player ?: return

        updateState(delta)
        if (currentState == State.DISABLED) {
            return
        }

        startRender()

        // get all furnaces in area around player
        val camPos = MC.camera.pos
        updateOrientation(player)

        val renderEntities = renderStates.filter {
            cameraDistanceSq(it.entity.pos) <= RENDER_DISTANCE_SQUARED
        }

        val renderSurfaces = mutableListOf<RenderSurface>()

        renderEntities.forEach {
            queueEntityRender(it, camPos, renderSurfaces)
        }

        this.renderSurfaces(renderSurfaces, camPos)

        endRender()
    }

    private fun updateState(delta: Float) {
        when (currentState) {
            State.STARTING -> {
                enableOpacity = min(1f, enableOpacity + delta * OPACITY_FADE_RATE)
                if (enableOpacity == 1f) {
                    currentState = State.ENABLED
                }
            }

            State.STOPPING -> {
                enableOpacity = max(0f, enableOpacity - delta * OPACITY_FADE_RATE)
                if (enableOpacity == 0f) {
                    currentState = State.DISABLED
                }
            }

            else -> {}
        }
    }

    private fun queueEntityRender(
            state: BlockRenderState<BlockEntity>,
            camPos: Vec3d,
            surfaces: MutableList<RenderSurface>
    ) {
        val rotation = currentRotation ?: Direction.NORTH
        val pos = state.entity.pos

        val stack = MatrixStack()
        stack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)

        Direction.values().forEach { side ->
            val blockOnFace = MC.currentWorld!!.getBlockState(pos.offset(side))
            if (blockOnFace.material.isSolid && blockOnFace.isOpaque) {
                // ignore block
                return@forEach
            }

            stack.push()
            translateToFace(stack, side, rotation)

            val center = Vector4f(0.5f, 0.0f, 0.5f, 1.0f)
            center.transform(stack.peek().model)
            // center is now relative to camera, calculate length
            val lengthSq = center.x * center.x + center.y * center.y + center.z * center.z
            // queue render
            surfaces.add(RenderSurface(state, side, lengthSq))
            stack.pop()
        }
    }

    private fun renderSurfaces(surfaces: List<RenderSurface>, camPos: Vec3d) {
        val rotation = currentRotation ?: Direction.NORTH

        surfaces.sortedByDescending { it.distanceSq }.forEach { surface ->
            val pos = surface.state.entity.pos

            val stack = MatrixStack()
            stack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)

            val blockDist = cameraDistanceSq(pos)
            val fadingAlpha = if (blockDist > FADING_DISTANCE_SQUARED) {
                val distance = sqrt(blockDist)
                val progress = max(0.0, RENDER_DISTANCE - distance)
                val scale = RENDER_DISTANCE - FADING_DISTANCE
                (progress / scale).toFloat()
            } else {
                1.0f
            }

            translateToFace(stack, surface.side, rotation)
            //stack.scale(1.005f, 1f, 1.005f)

            val alpha = fadingAlpha * enableOpacity

            val renderer = rendererRegistry[surface.state.entity.type]!!
            val bridge = RenderBridge(alpha, surface.state.entity, surface.side, rotation, surfaces)

            RenderSystem.pushMatrix()
            RenderSystem.multMatrix(stack.peek().model)

            renderer.render(surface.state, bridge)

            RenderSystem.popMatrix()
        }
    }

    private fun updateOrientation(entity: Entity) {
        val yaw = -entity.getYaw(1.0f) * 0.017453292f
        val x = MathHelper.sin(yaw)
        val z = MathHelper.cos(yaw)
        val xPos = x > 0.0f
        val zPos = z > 0.0f
        val absX = if (xPos) x else -x
        val absZ = if (zPos) z else -z
        val axisX =
            if (xPos) Direction.EAST else Direction.WEST
        val axisZ =
            if (zPos) Direction.SOUTH else Direction.NORTH

        val greaterAxis = if (absX > absZ) axisX else axisZ
        currentRotation = greaterAxis
    }
}