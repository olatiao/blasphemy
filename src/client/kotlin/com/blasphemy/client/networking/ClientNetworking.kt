package com.blasphemy.client.networking

import com.blasphemy.Blasphemy
import com.blasphemy.client.screen.DialogueScreen
import com.blasphemy.client.ui.QuestTracker
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 客户端网络处理
 */
object ClientNetworking {
    // 定义网络包ID常量 - 需与ServerNetworking匹配
    private val DIALOGUE_OPEN = Identifier("blasphemy", "dialogue_open")
    private val DIALOGUE_OPTION_SELECTED = Identifier("blasphemy", "dialogue_option_selected")
    private val QUEST_UPDATE = Identifier("blasphemy", "quest_update")
    
    // 新的包ID为NbtCompound格式数据
    private val DIALOGUE_DATA_PACKET_ID = Identifier("blasphemy", "dialogue_data")
    private val DIALOGUE_OPTION_PACKET_ID = Identifier("blasphemy", "dialogue_option")
    private val QUEST_DATA_PACKET_ID = Identifier("blasphemy", "quest_data")
    private val QUEST_PROGRESS_PACKET_ID = Identifier("blasphemy", "quest_progress")
    
    /**
     * 注册所有客户端网络接收器
     */
    fun registerReceivers() {
        // 注册旧的对话数据接收器（与服务端发送匹配）
        ClientPlayNetworking.registerGlobalReceiver(DIALOGUE_OPEN) { client, handler, buffer, sender ->
            try {
                Blasphemy.logger.info("接收到对话数据包")
                
                // 从数据包中读取数据
                val npcNameKey = buffer.readString()
                val dialogueTextKey = buffer.readString()
                val optionsCount = buffer.readInt()
                val optionKeys = (0 until optionsCount).map { buffer.readString() }
                
                Blasphemy.logger.info("对话数据: NPC=$npcNameKey, 文本=$dialogueTextKey, 选项数=$optionsCount")
                
                // 转换为适合UI展示的格式
                val npcName = npcNameKey // 在UI中会使用翻译键
                val dialogueText = dialogueTextKey // 在UI中会使用翻译键
                val options = optionKeys.toList() // 在UI中会使用翻译键
                
                // 在主线程中打开对话屏幕
                client.execute {
                    Blasphemy.logger.info("尝试打开对话屏幕")
                    try {
                        // 为了与服务端兼容，我们这里使用空字符串作为npcId和dialogueId
                        client.setScreen(DialogueScreen("", npcName, "", dialogueText, options))
                        Blasphemy.logger.info("对话屏幕已打开")
                    } catch (e: Exception) {
                        Blasphemy.logger.error("打开对话屏幕失败", e)
                    }
                }
            } catch (e: Exception) {
                Blasphemy.logger.error("处理对话数据包失败", e)
            }
        }
        
        // 保留新的对话数据接收器，以便将来使用
        ClientPlayNetworking.registerGlobalReceiver(DIALOGUE_DATA_PACKET_ID) { client, handler, buffer, sender ->
            val data = buffer.readNbt() ?: return@registerGlobalReceiver
            
            client.execute {
                Blasphemy.logger.info("接收到NbtCompound对话数据")
                openDialogueScreen(data)
            }
        }
        
        // 注册任务数据接收器
        ClientPlayNetworking.registerGlobalReceiver(QUEST_DATA_PACKET_ID) { client, handler, buffer, sender ->
            val data = buffer.readNbt() ?: return@registerGlobalReceiver
            
            client.execute {
                Blasphemy.logger.info("接收到任务数据")
                handleQuestData(data)
            }
        }
        
        // 注册任务更新接收器（与服务端匹配）
        ClientPlayNetworking.registerGlobalReceiver(QUEST_UPDATE) { client, handler, buffer, sender ->
            val questId = buffer.readString()
            val questNameKey = buffer.readString()
            val currentProgress = buffer.readInt()
            val maxProgress = buffer.readInt()
            val isCompleted = buffer.readBoolean()
            
            client.execute {
                Blasphemy.logger.info("接收到任务更新: $questId, 进度=$currentProgress/$maxProgress")
                
                // 更新任务进度
                QuestTracker.getInstance().updateQuestProgress(questId, currentProgress)
                
                // 显示任务进度通知
                if (isCompleted) {
                    client.player?.sendMessage(Text.translatable("quest.completed", Text.translatable(questNameKey)), false)
                }
            }
        }
        
        // 注册任务进度更新接收器
        ClientPlayNetworking.registerGlobalReceiver(QUEST_PROGRESS_PACKET_ID) { client, handler, buffer, sender ->
            val data = buffer.readNbt() ?: return@registerGlobalReceiver
            
            client.execute {
                Blasphemy.logger.info("接收到任务进度更新")
                updateQuestProgress(data)
            }
        }
        
        Blasphemy.logger.info("已注册客户端网络接收器")
    }
    
    /**
     * 打开对话屏幕 (新数据格式)
     */
    private fun openDialogueScreen(data: NbtCompound) {
        val npcId = data.getString("npcId")
        val npcName = data.getString("npcName")
        val dialogueId = data.getString("dialogueId")
        val dialogueText = data.getString("dialogueText")
        val options = mutableListOf<String>()
        
        // 读取选项
        val optionsNbt = data.getCompound("options")
        val optionCount = optionsNbt.getInt("count")
        
        for (i in 0 until optionCount) {
            options.add(optionsNbt.getString("option_$i"))
        }
        
        // 打开对话界面
        MinecraftClient.getInstance().setScreen(
            DialogueScreen(
                npcId,
                npcName,
                dialogueId,
                dialogueText,
                options
            )
        )
    }
    
    /**
     * 处理任务数据
     */
    private fun handleQuestData(data: NbtCompound) {
        val questId = data.getString("questId")
        val nameKey = data.getString("nameKey")
        val objectiveKey = data.getString("objectiveKey")
        val maxProgress = data.getInt("maxProgress")
        val npcNameKey = data.getString("npcNameKey")
        
        // 添加到任务追踪器
        QuestTracker.getInstance().addQuest(questId, nameKey, objectiveKey, maxProgress)
        
        // 设置NPC名称
        val quest = QuestTracker.getInstance().getActiveQuests().find { it.id == questId }
        quest?.npcNameKey = npcNameKey
    }
    
    /**
     * 更新任务进度
     */
    private fun updateQuestProgress(data: NbtCompound) {
        val questId = data.getString("questId")
        val progress = data.getInt("progress")
        
        // 更新任务进度
        QuestTracker.getInstance().updateQuestProgress(questId, progress)
    }
    
    /**
     * 发送选择对话选项的包 (旧格式，与服务端匹配)
     */
    fun sendDialogueOptionSelected(optionIndex: Int) {
        Blasphemy.logger.info("发送对话选项选择: $optionIndex")
        val buf = PacketByteBufs.create()
        buf.writeInt(optionIndex)
        ClientPlayNetworking.send(DIALOGUE_OPTION_SELECTED, buf)
    }
    
    /**
     * 发送选择对话选项的包 (新格式，使用NbtCompound)
     */
    fun sendDialogueOption(npcId: String, dialogueId: String, optionIndex: Int) {
        val buf = PacketByteBufs.create()
        val data = NbtCompound()
        
        data.putString("npcId", npcId)
        data.putString("dialogueId", dialogueId)
        data.putInt("optionIndex", optionIndex)
        
        buf.writeNbt(data)
        
        ClientPlayNetworking.send(DIALOGUE_OPTION_PACKET_ID, buf)
        Blasphemy.logger.info("发送对话选项：NPC=$npcId, 对话=$dialogueId, 选项=$optionIndex")
    }
} 