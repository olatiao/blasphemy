package com.blasphemy.enchantment

import com.blasphemy.config.ModConfig
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.Box
import net.minecraft.util.Formatting
import kotlin.math.cos
import kotlin.math.sin

/**
 * 群体斩击附魔，攻击时对目标周围的敌对生物也造成伤害
 */
class CleaveEnchantment(
    weight: Rarity,
    target: EnchantmentTarget = EnchantmentTarget.WEAPON,
    equipmentSlots: Array<EquipmentSlot> = arrayOf(EquipmentSlot.MAINHAND)
) : Enchantment(weight, target, equipmentSlots) {
    
    // 配置
    private val config = ModConfig.getConfig()

    /**
     * 获取最小附魔等级要求
     */
    override fun getMinPower(level: Int): Int {
        return 10 + (level - 1) * 10
    }

    /**
     * 获取最大附魔等级要求
     */
    override fun getMaxPower(level: Int): Int {
        return getMinPower(level) + 15
    }

    /**
     * 获取最大附魔等级
     */
    override fun getMaxLevel(): Int {
        return config.cleaveConfig.maxLevel
    }

    /**
     * 当攻击实体时触发附魔效果
     */
    override fun onTargetDamaged(user: LivingEntity, target: Entity, level: Int) {
        if (target !is LivingEntity || user !is PlayerEntity) return
        
        // 获取配置
        val range = config.cleaveConfig.attackRange
        val baseMultiplier = config.cleaveConfig.baseMultiplier
        val levelMultiplier = config.cleaveConfig.levelMultiplier
        
        // 计算本次伤害的倍率
        val damageMultiplier = baseMultiplier + ((level - 1) * levelMultiplier)
        
        // 获取围绕目标的一定范围内的敌对生物
        val world = user.world
        val box = Box(
            target.pos.x - range, target.pos.y - range, target.pos.z - range,
            target.pos.x + range, target.pos.y + range, target.pos.z + range
        )
        
        // 检查目标是否为敌对生物
        val targetIsHostile = target is HostileEntity
        
        // 获取除目标外的所有敌对生物
        val nearbyEntities = world.getEntitiesByClass(LivingEntity::class.java, box) { entity ->
            // 基本条件：不是自己，不是已攻击目标
            val basicCondition = entity != target && entity != user
            
            // 敌对生物筛选条件
            val hostileCondition = if (targetIsHostile) {
                // 如果攻击的目标是敌对生物，只影响敌对生物
                entity is HostileEntity
            } else {
                // 如果攻击的目标不是敌对生物，仍然只影响敌对生物
                entity is HostileEntity
            }
            
            // 同时满足基本条件和敌对条件
            basicCondition && hostileCondition
        }
        
        // 如果没有附近的敌人，直接返回
        if (nearbyEntities.isEmpty()) return
        
        // 显示效果提示
        if (!world.isClient) {
            user.sendMessage(
                Text.translatable("message.blasphemy.cleave.activate", nearbyEntities.size)
                    .formatted(Formatting.RED),
                true
            )
        }
        
        // 播放音效
        world.playSound(
            null,
            user.x, user.y, user.z,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
            SoundCategory.PLAYERS,
            1.0f,
            0.8f
        )
        
        // 对周围的敌人造成伤害
        for (entity in nearbyEntities) {
            // 根据原始伤害值计算群体伤害
            val originalDamage = if (user.mainHandStack.isDamageable) {
                // 获取武器基础伤害
                val attackDamageAttribute = user.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()
                
                // 预设基础伤害，如果无法获取
                attackDamageAttribute.coerceAtLeast(1.0f)
            } else {
                // 默认伤害
                1.0f
            }
            
            // 应用伤害
            val cleaveAmount = originalDamage * damageMultiplier
            entity.damage(user.damageSources.playerAttack(user), cleaveAmount)
            
            // 在服务端创建粒子效果
            if (!world.isClient && world is ServerWorld) {
                // 在被打击实体周围创建圆形粒子效果
                val particleCount = 12
                for (i in 0 until particleCount) {
                    val angle = (Math.PI * 2.0 * i) / particleCount
                    val distance = 0.5
                    val x = entity.x + cos(angle) * distance
                    val z = entity.z + sin(angle) * distance
                    
                    world.spawnParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        x, entity.y + 1.0, z,
                        1, 0.0, 0.0, 0.0, 0.0
                    )
                }
            }
        }
    }

    /**
     * 判断实体是否对玩家敌对
     */
    private fun isHostileToPlayer(entity: LivingEntity, player: PlayerEntity): Boolean {
        // 1. 如果是敌对生物，通常对玩家敌对
        if (entity is HostileEntity) {
            return true
        }
        
        // 2. 对于非敌对生物，只有显式地攻击了该生物才会受到影响
        // 这可以确保附魔只影响敌对生物，而不会伤害中立生物
        return false
    }

    /**
     * 确定是否可以接受此附魔
     */
    override fun canAccept(other: Enchantment): Boolean {
        // 可以与大多数武器附魔共存
        return super.canAccept(other)
    }
} 