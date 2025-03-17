package com.blasphemy

import com.blasphemy.client.networking.ClientNetworking
import com.blasphemy.client.render.NPCEntityRenderer
import com.blasphemy.registry.EntityRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import com.blasphemy.client.ClientEvents

object BlasphemyClient : ClientModInitializer {
	override fun onInitializeClient() {
		// 打印初始化日志
		Blasphemy.logger.info("客户端初始化...")
		
		// 先注册客户端网络处理器
		ClientNetworking.registerReceivers()
		Blasphemy.logger.info("客户端网络已注册")
		
		// 再注册实体渲染器
		EntityRendererRegistry.register(EntityRegistry.NPC_ENTITY, ::NPCEntityRenderer)
		Blasphemy.logger.info("NPC实体渲染器已注册")
		
		// 注册客户端事件处理器（包括任务追踪器渲染）
		ClientEvents.register()
		Blasphemy.logger.info("客户端事件处理器已注册")
		
		Blasphemy.logger.info("客户端初始化完成")
	}
}