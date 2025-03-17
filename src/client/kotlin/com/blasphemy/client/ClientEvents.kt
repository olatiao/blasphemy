package com.blasphemy.client

import com.blasphemy.Blasphemy
import com.blasphemy.client.screen.QuestCenterScreen
import com.blasphemy.client.ui.QuestTracker
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * 客户端事件处理器
 */
object ClientEvents {
    // 任务中心键绑定
    private lateinit var questCenterKeyBinding: KeyBinding
    
    /**
     * 注册所有客户端事件处理
     */
    fun register() {
        // 注册HUD渲染事件
        registerHudEvents()
        
        // 注册键绑定
        registerKeyBindings()
        
        Blasphemy.logger.info("已注册客户端事件处理器")
    }
    
    /**
     * 注册HUD渲染事件
     */
    private fun registerHudEvents() {
        // 注册任务追踪器渲染
        HudRenderCallback.EVENT.register { drawContext, tickDelta -> 
            QuestTracker.getInstance().render(drawContext)
        }
    }
    
    /**
     * 注册键绑定
     */
    private fun registerKeyBindings() {
        // 注册任务中心快捷键 - J键
        questCenterKeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.blasphemy.quest_center", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, // J键
                "category.blasphemy.keys" // 键绑定分类
            )
        )
        
        // 注册键绑定处理
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (questCenterKeyBinding.wasPressed()) {
                // 如果按下任务中心键，打开任务中心界面
                client.setScreen(QuestCenterScreen())
            }
        }
    }
} 