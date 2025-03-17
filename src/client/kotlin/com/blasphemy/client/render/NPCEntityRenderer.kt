package com.blasphemy.client.render

import com.blasphemy.entity.NPCEntity
import net.minecraft.client.render.entity.BipedEntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer
import net.minecraft.client.render.entity.model.BipedEntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.util.Identifier

/**
 * NPC实体渲染器
 */
class NPCEntityRenderer(ctx: EntityRendererFactory.Context) : 
    BipedEntityRenderer<NPCEntity, PlayerEntityModel<NPCEntity>>(
        ctx, 
        PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false),
        0.5f
    ) {
    
    init {
        // 添加盔甲渲染
        addFeature(
            ArmorFeatureRenderer(
                this,
                BipedEntityModel(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                BipedEntityModel(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.modelManager
            )
        )
    }
    
    override fun getTexture(entity: NPCEntity): Identifier {
        // 默认使用Steve的皮肤
        return Identifier("textures/entity/player/wide/steve.png")
    }
} 