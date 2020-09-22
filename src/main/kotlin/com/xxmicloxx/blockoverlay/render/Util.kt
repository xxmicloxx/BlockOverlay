package com.xxmicloxx.blockoverlay.render

import com.xxmicloxx.blockoverlay.providers.FontStorageProvider
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.FontStorage
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.Camera
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.client.util.math.Vector4f
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.resource.ResourceManager
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object MC {
    private val instance: MinecraftClient
        get() = MinecraftClient.getInstance()

    val camera: Camera
        get() = instance.gameRenderer.camera

    val player: PlayerEntity?
        get() = instance.player

    val currentWorld: World?
        get() = instance.world

    val currentScreen: Screen?
        get() = instance.currentScreen

    val gameRenderer: GameRenderer
        get() = instance.gameRenderer

    val lightmapTextureManager: LightmapTextureManager
        get() = gameRenderer.lightmapTextureManager

    val resourceManager: ResourceManager
        get() = instance.resourceManager

    val textureManager: TextureManager
        get() = instance.textureManager

    @Suppress("CAST_NEVER_SUCCEEDS")
    val fontStorage: FontStorage
        get() = (instance.textRenderer as FontStorageProvider).blockoverlay_getFontStorage()
}

fun translateToFace(stack: MatrixStack, face: Direction, rotation: Direction? = null) = stack.apply {
    multiply(face.rotationQuaternion)

    fun rotateToPlayer(down: Boolean) {
        rotation ?: return

        val axis = if (down) Vector3f.NEGATIVE_Y else Vector3f.POSITIVE_Y
        when (rotation) {
            Direction.NORTH -> {}
            Direction.EAST -> {
                if (!down)
                    translate(1.0, 0.0, 0.0)
                else
                    translate(0.0, 0.0, 1.0)

                multiply(axis.getDegreesQuaternion(-90f))
            }
            Direction.SOUTH -> {
                translate(1.0, 0.0, 1.0)
                multiply(axis.getDegreesQuaternion(180f))
            }
            Direction.WEST -> {
                if (!down)
                    translate(0.0, 0.0, 1.0)
                else
                    translate(1.0, 0.0, 0.0)

                multiply(axis.getDegreesQuaternion(90f))
            }
            else -> return
        }
    }

    when (face) {
        Direction.UP -> {
            translate(0.0, 1.0, 0.0)
            rotateToPlayer(false)
        }
        Direction.DOWN -> {
            translate(0.0, 0.0, -1.0)
            rotateToPlayer(true)
        }
        Direction.NORTH -> translate(-1.0, 0.0, -1.0)
        Direction.EAST -> translate(-1.0, 1.0, -1.0)
        Direction.SOUTH -> translate(0.0, 1.0, -1.0)
        Direction.WEST -> translate(0.0, 0.0, -1.0)
    }
}

fun colorVectorToInt(color: Vector3f, alpha: Float): Int {
    val r = (color.x * 255).toInt() shl 16
    val g = (color.y * 255).toInt() shl 8
    val b = (color.z * 255).toInt()
    val a = (alpha * 255).toInt() shl 24

    return r or g or b or a
}

fun colorVectorToInt(color: Vector4f, alpha: Float): Int {
    return colorVectorToInt(Vector3f(color.x, color.y, color.z), alpha)
}

fun colorVectorToInt(color: Vector4f): Int {
    return colorVectorToInt(color, color.w)
}

fun colorVectorToInt(color: Vector3f): Int {
    return colorVectorToInt(color, 1.0f)
}