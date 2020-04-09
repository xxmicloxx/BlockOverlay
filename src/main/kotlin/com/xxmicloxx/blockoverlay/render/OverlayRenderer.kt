package com.xxmicloxx.blockoverlay.render

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.ContainerHelper
import com.xxmicloxx.blockoverlay.render.block.BlockOverlayRenderer
import com.xxmicloxx.blockoverlay.render.block.FurnaceOverlayRenderer
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import kotlin.math.*

object OverlayRenderer {
    private const val CACHE_DISTANCE = 16.0
    private const val CACHE_DISTANCE_SQUARED = CACHE_DISTANCE * CACHE_DISTANCE

    private const val RENDER_DISTANCE = 5.0
    private const val RENDER_DISTANCE_SQUARED = RENDER_DISTANCE * RENDER_DISTANCE

    private const val FADING_DISTANCE = 4.75
    private const val FADING_DISTANCE_SQUARED = FADING_DISTANCE * FADING_DISTANCE

    private const val OPACITY_FADE_RATE = 3.0f

    enum class State(val enabled: Boolean) {
        STARTING(true),
        ENABLED(true),
        STOPPING(false),
        DISABLED(false)
    }

    private val rendererRegistry = mutableMapOf<BlockEntityType<out BlockEntity>, BlockOverlayRenderer>()
    private val renderStates = mutableListOf<BlockRenderState<BlockEntity>>()
    private var lastWorld: World? = null

    var currentState = State.DISABLED
        private set

    private var enableOpacity = 0.0f

    private val sideOpacity = arrayOf(0f, 0f, 0f, 0f, 0f, 0f)
    private var currentSide: Direction? = null
    private var currentRotation: Direction? = null
    private var orientationLocked: Boolean = false

    private val visibleSides: List<Direction>
        get() = sideOpacity.indices.filter { sideOpacity[it] != 0f }.map { Direction.byId(it) }

    private val hasVisibleSide: Boolean
        get() = visibleSides.isNotEmpty()


    fun enable() {
        if (currentState != State.ENABLED && currentState != State.STARTING) {
            currentState = State.STARTING
            ContainerHelper.resume()
        }
    }

    fun disable() {
        if (currentState != State.DISABLED && currentState != State.STOPPING) {
            currentState = State.STOPPING
            orientationLocked = true
            ContainerHelper.pause()
        }
    }

    init {
        registerRenderer(FurnaceOverlayRenderer())
    }

    private fun registerRenderer(renderer: BlockOverlayRenderer) {
        renderer.matchedBlocks.forEach {
            rendererRegistry.putIfAbsent(it, renderer)
        }
    }

    private fun destroyState() {
        renderStates.forEach { it.destroy() }
        renderStates.clear()

        sideOpacity.indices.forEach { sideOpacity[it] = 0f }
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

        // get all relevant entities
        val player = MC.player ?: return

        val entities = world.blockEntities
            .filter { rendererRegistry.containsKey(it.type) }
            .filter { player.squaredDistanceTo(it.pos.x + 0.5, it.pos.y + 0.5, it.pos.z + 0.5) <=
                    CACHE_DISTANCE_SQUARED }
            .toHashSet()

        // destroy states which are not in entity list
        renderStates
            .filter { !entities.contains(it.entity) }
            .forEach {
                it.destroy()
                renderStates.remove(it)
            }

        if (currentState != State.STOPPING) {
            // add states that should be displayed
            entities
                .filter {player.squaredDistanceTo(it.pos.x + 0.5, it.pos.y + 0.5, it.pos.z + 0.5) <=
                        RENDER_DISTANCE_SQUARED }
                .filter { matchEntity -> renderStates.none { it.entity == matchEntity } }
                .forEach {
                    val renderer = rendererRegistry[it.type] ?: return@forEach
                    renderStates.add(renderer.createRenderState(it))
                }
        }
    }

    private fun startRender() {
        RenderSystem.enableDepthTest()

        RenderSystem.shadeModel(GL11.GL_SMOOTH)

        RenderSystem.disableAlphaTest()
        RenderSystem.defaultAlphaFunc()

        RenderSystem.disableTexture()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
    }

    private fun endRender() {
        RenderSystem.enableAlphaTest()
        RenderSystem.enableBlend()
        RenderSystem.enableTexture()
        RenderSystem.enableDepthTest()
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
        updateOpacity(delta)

        val renderEntities = renderStates.filter {
            player.squaredDistanceTo(it.entity.pos.x + 0.5, it.entity.pos.y + 0.5, it.entity.pos.z + 0.5) <=
                    RENDER_DISTANCE_SQUARED
        }

        renderEntities.forEach {
            renderEntity(it, player, camPos)
        }

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
                    orientationLocked = false
                }
            }

            else -> {}
        }
    }

    private fun renderEntity(
        state: BlockRenderState<BlockEntity>,
        player: PlayerEntity,
        camPos: Vec3d
    ) {
        val rotation = currentRotation ?: Direction.NORTH
        val pos = state.entity.pos
        val renderSides = visibleSides

        val distanceSquared =
            player.squaredDistanceTo(state.entity.pos.x + 0.5, state.entity.pos.y + 0.5, state.entity.pos.z + 0.5)

        val fadingAlpha = if (distanceSquared > FADING_DISTANCE_SQUARED) {
            val distance = sqrt(distanceSquared)
            val progress = RENDER_DISTANCE - distance
            val scale = RENDER_DISTANCE - FADING_DISTANCE
            (progress / scale).toFloat()
        } else {
            1.0f
        }

        val stack = MatrixStack()
        stack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)

        renderSides.forEach { renderSide ->
            stack.push()
            translateToFace(stack, renderSide, rotation)
            stack.translate(0.0, 0.001, 0.0)

            val alpha = fadingAlpha * sideOpacity[renderSide.id] * enableOpacity

            RenderSystem.pushMatrix()
            RenderSystem.multMatrix(stack.peek().model)
            val renderer = rendererRegistry[state.entity.type]!!
            renderer.render(state, alpha)
            RenderSystem.popMatrix()
            stack.pop()
        }
    }

    fun lockOrientation() {
        orientationLocked = true

        // snap orientation
        currentSide = null
        updateOrientation(MC.player ?: return)
    }

    fun unlockOrientation() {
        orientationLocked = false
    }

    private fun updateOrientation(entity: Entity) {
        if (orientationLocked && currentSide != null) {
            return
        }

        val verticalThreshold = 60 / 180f * PI.toFloat()
        val horizontalThreshold = 70 / 180f * PI.toFloat()
        val horizontalVecThreshold = MathHelper.sin(horizontalThreshold)

        val pitch = entity.getPitch(1.0f) * 0.017453292f
        val yaw = -entity.getYaw(1.0f) * 0.017453292f
        val y = MathHelper.sin(pitch)
        val x = MathHelper.sin(yaw)
        val z = MathHelper.cos(yaw)
        val xPos = x > 0.0f
        val yPos = y < 0.0f
        val zPos = z > 0.0f
        val absX = if (xPos) x else -x
        val absZ = if (zPos) z else -z
        val axisX =
            if (xPos) Direction.EAST else Direction.WEST
        val axisY =
            if (yPos) Direction.UP else Direction.DOWN
        val axisZ =
            if (zPos) Direction.SOUTH else Direction.NORTH

        val greaterAxis = if (absX > absZ) axisX else axisZ
        if (abs(pitch) > verticalThreshold) {
            currentSide = axisY.opposite
            currentRotation = greaterAxis
        } else {
            if (currentSide?.axis == Direction.Axis.Y || currentSide == null) {
                currentSide = greaterAxis.opposite
            }

            val greaterAbs = max(absX, absZ)
            if (greaterAbs > horizontalVecThreshold) {
                // snap using threshold grid
                currentSide = greaterAxis.opposite
            }
        }
    }

    private fun updateOpacity(delta: Float) {
        val renderSide = currentSide ?: return
        val opacityDelta = OPACITY_FADE_RATE * delta

        if (hasVisibleSide) {
            sideOpacity[renderSide.id] = min(1.0f, sideOpacity[renderSide.id] + opacityDelta)
            sideOpacity.indices
                .filter { it != renderSide.id }
                .forEach { sideOpacity[it] = max(0f, sideOpacity[it] - opacityDelta) }
        } else {
            sideOpacity[renderSide.id] = 1.0f
        }
    }
}