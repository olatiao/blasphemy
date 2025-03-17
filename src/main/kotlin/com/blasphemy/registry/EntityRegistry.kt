package com.blasphemy.registry

import com.blasphemy.Blasphemy
import com.blasphemy.entity.NPCEntity
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * 实体注册类
 */
object EntityRegistry {
    // 注册NPC实体
    val NPC_ENTITY: EntityType<NPCEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier(Blasphemy.MOD_ID, "npc"),
        FabricEntityTypeBuilder.create<NPCEntity>(SpawnGroup.MISC) { entityType, world ->
            NPCEntity(entityType, world)
        }
            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
            .trackRangeBlocks(32)
            .build()
    )
    
    /**
     * 注册实体属性
     */
    fun registerAttributes() {
        // 注册NPC实体属性
        FabricDefaultAttributeRegistry.register(NPC_ENTITY, NPCEntity.createNpcAttributes())
        
        Blasphemy.logger.info("注册实体属性完成")
    }
} 