package com.xxmicloxx.blockoverlay.render

import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11

class DrawCache {
    private var glId: Int? = null

    val isCached: Boolean
        get() = glId != null

    private fun resetState() {
        val tex = MC.textureManager.getTexture(Textures.ATLAS_ID) ?: return
        tex.bindTexture()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.glId)
        RenderSystem.disableLighting()
        GL11.glDisable(GL11.GL_LIGHTING)
    }

    fun startDrawing(execute: Boolean) {
        val id = glId ?: GL11.glGenLists(1)
        glId = id

        val mode = if (execute) GL11.GL_COMPILE_AND_EXECUTE else GL11.GL_COMPILE
        GL11.glNewList(id, mode)
        resetState()
    }

    fun finishDrawing() {
        GL11.glEndList()
    }

    fun draw() {
        val id = glId ?: throw IllegalStateException("Cache is empty")

        resetState()
        GL11.glCallList(id)
        resetState()
    }

    fun destroy() {
        val id = glId ?: return
        GL11.glDeleteLists(id, 1)
        glId = null
    }
}