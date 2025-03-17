package com.blasphemy.network

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import com.blasphemy.dialogue.DialogueManager
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 服务器网络通信类
 */
object ServerNetworking {
    // 网络通道标识符
    val DIALOGUE_OPEN = Identifier("blasphemy", "dialogue_open")
    val DIALOGUE_OPTION_SELECTED = Identifier("blasphemy", "dialogue_option_selected")
    val QUEST_UPDATE = Identifier("blasphemy", "quest_update")
    
    /**
     * 注册服务端网络处理器
     */
    fun registerServerNetworking() {
        Blasphemy.logger.info("注册服务端网络处理器")
        
        // 注册对话选项选择数据包处理器
        ServerPlayNetworking.registerGlobalReceiver(DIALOGUE_OPTION_SELECTED) { 
            server: MinecraftServer, 
            player: ServerPlayerEntity, 
            _: ServerPlayNetworkHandler,
            buf: PacketByteBuf, 
            _: PacketSender ->
            
            Blasphemy.logger.info("收到对话选项选择数据包")
            val optionIndex = buf.readInt()
            
            // 在服务器线程中处理选项
            server.execute {
                DialogueManager.selectOption(player, optionIndex)
            }
        }
        
        Blasphemy.logger.info("服务端网络处理器注册完成")
    }
    
    /**
     * 向玩家发送对话数据
     */
    fun sendDialogueToPlayer(
        player: PlayerEntity,
        npcNameKey: String,
        dialogueTextKey: String,
        optionKeys: List<String>
    ) {
        if (player !is ServerPlayerEntity) return
        
        Blasphemy.logger.info("发送对话数据到玩家: ${player.name.string}")
        Blasphemy.logger.info("对话内容: NPC=$npcNameKey, 文本=$dialogueTextKey, 选项数=${optionKeys.size}")
        
        try {
            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeString(npcNameKey)
            buf.writeString(dialogueTextKey)
            buf.writeInt(optionKeys.size)
            
            optionKeys.forEach { buf.writeString(it) }
            
            ServerPlayNetworking.send(player, DIALOGUE_OPEN, buf)
            Blasphemy.logger.info("对话数据发送成功")
        } catch (e: Exception) {
            Blasphemy.logger.error("发送对话数据失败", e)
        }
    }
    
    /**
     * 向玩家发送任务更新数据
     */
    fun sendQuestUpdateToPlayer(
        player: ServerPlayerEntity,
        questId: String,
        questNameKey: String,
        currentProgress: Int,
        maxProgress: Int,
        isCompleted: Boolean
    ) {
        Blasphemy.logger.info("发送任务更新数据: ${player.name.string}, $questId, 进度=$currentProgress/$maxProgress")
        
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeString(questId)
        buf.writeString(questNameKey)
        buf.writeInt(currentProgress)
        buf.writeInt(maxProgress)
        buf.writeBoolean(isCompleted)
        
        ServerPlayNetworking.send(player, QUEST_UPDATE, buf)
    }
} 