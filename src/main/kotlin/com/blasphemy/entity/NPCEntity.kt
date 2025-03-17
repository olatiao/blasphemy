package com.blasphemy.entity

import com.blasphemy.dialogue.DialogueManager
import net.minecraft.entity.EntityType
import net.minecraft.entity.Npc
import net.minecraft.entity.ai.goal.LookAroundGoal
import net.minecraft.entity.ai.goal.LookAtEntityGoal
import net.minecraft.entity.ai.goal.SwimGoal
import net.minecraft.entity.ai.goal.WanderAroundGoal
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

/**
 * NPC实体类
 */
class NPCEntity(entityType: EntityType<out NPCEntity>, world: World) : 
    PathAwareEntity(entityType, world), Npc {
    
    private var npcType: String = "village_chief"
    private var customName: Text? = null
    
    init {
        isCustomNameVisible = true
    }
    
    override fun initGoals() {
        goalSelector.add(0, SwimGoal(this))
        goalSelector.add(1, LookAtEntityGoal(this, PlayerEntity::class.java, 8.0f))
        goalSelector.add(2, WanderAroundGoal(this, 0.5))
        goalSelector.add(3, LookAroundGoal(this))
    }
    
    override fun interactMob(player: PlayerEntity, hand: Hand): ActionResult {
        if (world.isClient) {
            return ActionResult.SUCCESS
        }
        
        if (player.getStackInHand(hand).isEmpty) {
            // 发送简单消息
            player.sendMessage(Text.literal("你点击了 ${customName?.string ?: "NPC"} - 交互测试"), false)
            
            // 使用简单的对话方式
            player.sendMessage(Text.translatable("dialogue.blasphemy.village_chief.greeting"), false)
            player.sendMessage(Text.translatable("dialogue.option.blasphemy.whats_in_it"), false)
            player.sendMessage(Text.translatable("dialogue.option.blasphemy.sure"), false)
            player.sendMessage(Text.translatable("dialogue.option.blasphemy.no"), false)
            
            // 开始对话
            DialogueManager.startDialogue(player, npcType)
            return ActionResult.SUCCESS
        }
        
        return ActionResult.PASS
    }
    
    fun setNpcType(type: String) {
        this.npcType = type
        updateCustomName()
    }
    
    private fun updateCustomName() {
        this.customName = Text.translatable("npc.blasphemy.$npcType")
    }
    
    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.putString("NpcType", npcType)
    }
    
    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        if (nbt.contains("NpcType")) {
            npcType = nbt.getString("NpcType")
            updateCustomName()
        }
    }
    
    override fun getCustomName(): Text? {
        return customName ?: super.getCustomName()
    }
    
    companion object {
        /**
         * 创建NPC实体属性
         */
        fun createNpcAttributes(): DefaultAttributeContainer.Builder {
            return createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
        }
    }
} 