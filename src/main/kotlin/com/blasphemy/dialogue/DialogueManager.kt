package com.blasphemy.dialogue

import com.blasphemy.Blasphemy
import com.blasphemy.network.ServerNetworking
import com.blasphemy.quest.Quest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理NPC对话和任务系统
 */
object DialogueManager {
    private val dialogues = ConcurrentHashMap<String, DialogueTree>()
    private val activeDialogues = ConcurrentHashMap<UUID, String>()
    private val playerActiveQuests = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val playerCompletedQuests = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val quests = ConcurrentHashMap<String, Quest>()

    private val gson = Gson()

    /**
     * 从资源文件加载对话和任务配置
     */
    fun loadDialogues(resourceManager: ResourceManager) {
        dialogues.clear()
        quests.clear()
        
        Blasphemy.logger.info("开始加载对话配置...")
        
        // 修改资源查找方式，使用更明确的路径模式
        val dialogueConfigPath = Identifier(Blasphemy.MOD_ID, "dialogue_config.json")
        val resource = resourceManager.getResource(dialogueConfigPath)
        
        if (resource.isPresent) {
            try {
                loadDialogueFile(resource.get())
                Blasphemy.logger.info("成功加载对话配置: $dialogueConfigPath")
                
                // 打印加载的对话信息
                dialogues.forEach { (id, tree) ->
                    Blasphemy.logger.info("已加载NPC对话: $id, 起始对话: ${tree.startDialogue}, 对话数量: ${tree.dialogues.size}")
                }
            } catch (e: Exception) {
                Blasphemy.logger.error("加载对话配置文件失败: $dialogueConfigPath", e)
            }
        } else {
            Blasphemy.logger.error("找不到对话配置文件: $dialogueConfigPath")
            
            // 尝试查找可能的对话配置文件
            val possibleFiles = resourceManager.findResources("data") { path ->
                path.path.contains("dialogue") && path.path.endsWith(".json")
            }
            
            if (possibleFiles.isNotEmpty()) {
                Blasphemy.logger.info("找到以下可能的对话文件:")
                possibleFiles.forEach { (id, _) ->
                    Blasphemy.logger.info(" - $id")
                }
            } else {
                Blasphemy.logger.info("没有找到任何可能的对话配置文件")
            }
        }
    }
    
    private fun loadDialogueFile(resource: Resource) {
        InputStreamReader(resource.inputStream).use { reader ->
            val jsonObject = JsonParser.parseReader(reader).asJsonObject
            
            // 加载对话
            if (jsonObject.has("dialogueConfig")) {
                val dialogueConfig = jsonObject.getAsJsonObject("dialogueConfig")
                val npcId = dialogueConfig.get("id").asString
                
                val dialogueEntries = jsonObject.getAsJsonObject("dialogues").entrySet()
                    .associate { (id, dialogueJson) ->
                        val dialogue = gson.fromJson(dialogueJson, DialogueEntry::class.java)
                        id to dialogue
                    }
                    
                dialogues[npcId] = DialogueTree(
                    id = npcId,
                    nameKey = dialogueConfig.get("nameKey").asString,
                    startDialogue = dialogueConfig.get("startDialogue").asString,
                    dialogues = dialogueEntries
                )
            }
            
            // 加载任务
            if (jsonObject.has("questConfig")) {
                val questEntries = jsonObject.getAsJsonObject("questConfig").entrySet()
                questEntries.forEach { (id, questJson) ->
                    val quest = gson.fromJson(questJson, Quest::class.java)
                    quests[quest.id] = quest
                }
            }
        }
    }

    /**
     * 开始与NPC的对话
     */
    fun startDialogue(player: PlayerEntity, npcId: String): Boolean {
        Blasphemy.logger.info("开始与NPC对话: $npcId, 玩家: ${player.name.string}")
        
        val dialogue = dialogues[npcId]
        if (dialogue == null) {
            Blasphemy.logger.error("找不到NPC对话配置: $npcId")
            return false
        }
        
        activeDialogues[player.uuid] = npcId
        
        // 返回初始对话
        val startDialogueId = dialogue.startDialogue
        return sendDialogue(player, startDialogueId)
    }
    
    /**
     * 发送指定ID的对话给玩家
     */
    fun sendDialogue(player: PlayerEntity, dialogueId: String): Boolean {
        val npcId = activeDialogues[player.uuid] ?: return false
        val dialogue = dialogues[npcId] ?: return false
        val entry = dialogue.dialogues[dialogueId] ?: return false
        
        Blasphemy.logger.info("发送对话给玩家: $dialogueId")
        
        // 通过网络发送对话到客户端
        if (player is ServerPlayerEntity) {
            // 获取NPC名称和对话选项
            val npcNameKey = dialogue.nameKey
            val optionKeys = entry.options.map { it.textKey }
            
            // 临时在聊天栏显示对话内容
            player.sendMessage(Text.literal("[调试] 对话内容: ${entry.text}"), false)
            
            // 使用ServerNetworking发送对话数据到客户端，打开GUI界面
            ServerNetworking.sendDialogueToPlayer(
                player,
                npcNameKey,
                entry.text,
                optionKeys
            )
        }
        
        return true
    }
    
    /**
     * 选择对话选项
     */
    fun selectOption(player: PlayerEntity, optionIndex: Int): Boolean {
        val npcId = activeDialogues[player.uuid] ?: return false
        val dialogue = dialogues[npcId] ?: return false
        val currentDialogueId = activeDialogues[player.uuid] ?: return false
        val currentEntry = dialogue.dialogues[currentDialogueId] ?: return false
        
        Blasphemy.logger.info("玩家选择了选项: $optionIndex, 当前对话: $currentDialogueId")
        
        // 检查选项索引是否有效
        if (optionIndex < 0 || optionIndex >= currentEntry.options.size) {
            Blasphemy.logger.error("无效的选项索引: $optionIndex")
            return false
        }
        
        // 获取所选选项
        val selectedOption = currentEntry.options[optionIndex]
        
        // 处理选项动作
        when (selectedOption.action) {
            "END" -> {
                // 结束对话
                endDialogue(player)
                return true
            }
        }
        
        // 处理任务动作
        if (selectedOption.questAction != null && selectedOption.questId != null && player is ServerPlayerEntity) {
            when (selectedOption.questAction) {
                "ACCEPT" -> {
                    // 接受任务
                    acceptQuest(player, selectedOption.questId)
                }
                "DECLINE" -> {
                    // 拒绝任务
                    player.sendMessage(Text.translatable("dialogue.quest.declined"), false)
                }
            }
        }
        
        // 跳转到下一个对话
        if (selectedOption.next != null) {
            return sendDialogue(player, selectedOption.next)
        } else if (currentEntry.next != null) {
            return sendDialogue(player, currentEntry.next)
        }
        
        // 如果没有下一个对话，则结束对话
        endDialogue(player)
        return true
    }
    
    /**
     * 结束对话
     */
    fun endDialogue(player: PlayerEntity) {
        activeDialogues.remove(player.uuid)
        player.sendMessage(Text.translatable("dialogue.ended"), true)
    }
    
    /**
     * 接受任务
     */
    fun acceptQuest(player: ServerPlayerEntity, questId: String): Boolean {
        if (!quests.containsKey(questId)) return false
        
        val playerQuests = playerActiveQuests.computeIfAbsent(player.uuid) { mutableSetOf() }
        if (playerQuests.contains(questId)) {
            player.sendMessage(Text.translatable("quest.already.accepted"), true)
            return false
        }
        
        playerQuests.add(questId)
        val quest = quests[questId]!!
        
        player.sendMessage(Text.translatable("quest.accepted", Text.translatable(quest.nameKey)), false)
        player.sendMessage(Text.translatable("quest.objective", Text.translatable(quest.objectiveKey)), false)
        
        return true
    }
    
    /**
     * 检查任务进度
     */
    fun checkQuestProgress(player: ServerPlayerEntity, type: String, targetId: String, amount: Int = 1) {
        val playerQuests = playerActiveQuests[player.uuid] ?: return
        
        for (questId in playerQuests) {
            val quest = quests[questId] ?: continue
            
            if (quest.type == type && quest.targetId == targetId) {
                // TODO: 更新任务进度
            }
        }
    }
    
    /**
     * 检查玩家是否有活跃的任务
     */
    fun hasActiveQuest(player: PlayerEntity, questId: String): Boolean {
        return playerActiveQuests[player.uuid]?.contains(questId) == true
    }
    
    /**
     * 检查玩家是否完成了任务
     */
    fun hasCompletedQuest(player: PlayerEntity, questId: String): Boolean {
        return playerCompletedQuests[player.uuid]?.contains(questId) == true
    }
}

/**
 * 对话树数据结构
 */
data class DialogueTree(
    val id: String,
    val nameKey: String,
    val startDialogue: String,
    val dialogues: Map<String, DialogueEntry>
)

/**
 * 对话条目
 */
data class DialogueEntry(
    val text: String,
    val options: List<DialogueOption> = emptyList(),
    val questId: String? = null,
    val next: String? = null
)

/**
 * 对话选项
 */
data class DialogueOption(
    val textKey: String,
    val action: String? = null,
    val next: String? = null,
    val questAction: String? = null,
    val questId: String? = null
)

/**
 * 对话状态类，记录玩家当前的对话状态
 */
data class DialogueState(
    val npcId: String,
    var currentDialogueId: String,
    val player: PlayerEntity
)

/**
 * 对话选项类型
 */
enum class DialogueOptionType {
    CONTINUE,      // 继续对话
    QUEST_ACCEPT,  // 接受任务
    QUEST_DECLINE, // 拒绝任务
    END            // 结束对话
} 