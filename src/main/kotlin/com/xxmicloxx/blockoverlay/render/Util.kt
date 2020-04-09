package com.xxmicloxx.blockoverlay.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.render.Camera
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.entity.player.PlayerEntity
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
}

fun translateToFace(stack: MatrixStack, face: Direction, rotation: Direction? = null) = stack.apply {
    multiply(face.rotationQuaternion)

    fun rotateToPlayer(down: Boolean) {
        rotation ?: return@rotateToPlayer

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
            else -> return@rotateToPlayer
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