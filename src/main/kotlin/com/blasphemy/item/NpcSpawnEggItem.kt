package com.blasphemy.item

import com.blasphemy.entity.NPCEntity
import com.blasphemy.registry.EntityRegistry
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.math.Direction

/**
 * NPC生成蛋物品
 */
class NpcSpawnEggItem(settings: Settings) : Item(settings) {
    
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        if (world.isClient) {
            return ActionResult.SUCCESS
        }
        
        val itemStack = context.stack
        val pos = context.blockPos
        val direction = context.side
        val spawnPos = if (direction == Direction.DOWN) {
            pos.down()
        } else {
            pos.offset(direction)
        }
        
        if (world is ServerWorld) {
            // 创建实体
            val entity = EntityRegistry.NPC_ENTITY.create(world)
            
            if (entity != null) {
                // 设置实体位置
                entity.refreshPositionAndAngles(
                    spawnPos.x + 0.5,
                    spawnPos.y.toDouble(),
                    spawnPos.z + 0.5,
                    0f,
                    0f
                )
                
                // 设置NPC类型
                val npcType = if (itemStack.hasNbt() && itemStack.nbt!!.contains("NpcType")) {
                    itemStack.nbt!!.getString("NpcType")
                } else {
                    "village_chief"
                }
                entity.setNpcType(npcType)
                
                // 生成实体
                world.spawnEntity(entity)
                
                // 消耗物品（非创造模式）
                if (context.player != null && !context.player!!.abilities.creativeMode) {
                    itemStack.decrement(1)
                }
            }
        }
        
        return ActionResult.CONSUME
    }
    
    /**
     * 设置NPC类型到物品
     */
    fun setNpcType(stack: ItemStack, npcType: String): ItemStack {
        val nbt = stack.orCreateNbt
        nbt.putString("NpcType", npcType)
        return stack
    }
}