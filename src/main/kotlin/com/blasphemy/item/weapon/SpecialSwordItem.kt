package com.blasphemy.item.weapon

import com.blasphemy.Blasphemy
import com.blasphemy.compat.LeylinesCompat
import com.blasphemy.config.ModConfig
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.tag.TagKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.UUID
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import net.minecraft.server.world.ServerWorld

/**
 * 特殊剑武器类，包含特殊能力的剑
 */
class SpecialSwordItem(
    material: ToolMaterial,
    attackDamage: Int,
    attackSpeed: Float,
    settings: Settings,
    private val swordType: SwordType
) : MagicSwordItem(material, attackDamage, attackSpeed, settings) {

    // 配置
    private val config = ModConfig.getConfig()

    enum class SwordType {
        SERAPHIM,   // 天使剑
        RAPIDS,     // 急流剑
        MURAMASA    // 村正剑
    }

    // 用于存储每个玩家使用技能的冷却时间
    private val cooldowns = mutableMapOf<UUID, Long>()
    
    // 根据配置获取天使剑的冷却时间（毫秒）
    private val seraphimCooldown: Long
        get() = config.seraphimSword.cooldownSeconds * 1000L

    override fun useOnEntity(stack: ItemStack, user: PlayerEntity, entity: LivingEntity, hand: Hand): ActionResult {
        // 普通的交互处理
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        
        // 添加武器特殊效果提示
        val tooltipKey = "item.blasphemy.${getTooltipIdBySwordType()}.tooltip"
        tooltip.add(Text.translatable(tooltipKey))
    }
    
    /**
     * 根据剑类型获取提示ID
     */
    private fun getTooltipIdBySwordType(): String {
        return when (swordType) {
            SwordType.SERAPHIM -> "seraphim_sword"
            SwordType.RAPIDS -> "rapids_sword"
            SwordType.MURAMASA -> "muramasa_sword"
        }
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        
        // 天使剑技能：推开敌人并造成伤害
        if (swordType == SwordType.SERAPHIM) {
            val currentTime = System.currentTimeMillis()
            val lastUseTime = cooldowns[user.uuid] ?: 0L
            
            // 检查冷却时间
            if (currentTime - lastUseTime >= seraphimCooldown) {
                // 客户端粒子效果
                if (world.isClient && config.seraphimSword.enableParticles) {
                    // 生成环绕光环粒子效果
                    val radius = 2.0
                    val particlesPerRing = 20
                    val rings = 3
                    
                    // 添加闪光中心效果
                    world.addParticle(
                        ParticleTypes.FLASH,
                        user.x,
                        user.y + 1.0,
                        user.z,
                        0.0,
                        0.0,
                        0.0
                    )
                    
                    // 创建粒子光环
                    for (ring in 0 until rings) {
                        val ringHeight = 0.2 * ring
                        val ringRadius = radius - (ring * 0.5)
                        
                        for (i in 0 until particlesPerRing) {
                            val angle = (Math.PI * 2.0 * i) / particlesPerRing
                            val x = user.x + cos(angle) * ringRadius
                            val z = user.z + sin(angle) * ringRadius
                            
                            world.addParticle(
                                ParticleTypes.END_ROD,
                                x,
                                user.y + 1.0 + ringHeight,
                                z,
                                0.0,
                                0.05,
                                0.0
                            )
                        }
                    }
                    
                    // 添加随机扩散粒子
                    for (i in 0 until config.seraphimSword.particleCount) {
                        world.addParticle(
                            ParticleTypes.END_ROD,
                            user.x + (world.random.nextDouble() - 0.5) * 2,
                            user.y + 1.0,
                            user.z + (world.random.nextDouble() - 0.5) * 2,
                            (world.random.nextDouble() - 0.5) * 0.2,
                            world.random.nextDouble() * 0.2,
                            (world.random.nextDouble() - 0.5) * 0.2
                        )
                    }
                    
                    // 添加额外的光束效果
                    for (i in 0 until 8) {
                        val angle = (Math.PI * 2.0 * i) / 8
                        val distance = 5.0
                        val x = user.x + cos(angle) * distance
                        val z = user.z + sin(angle) * distance
                        
                        world.addParticle(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            x,
                            user.y,
                            z,
                            0.0,
                            0.05,
                            0.0
                        )
                    }
                }
                
                if (!world.isClient) {
                    // 获取5格范围内的敌对生物
                    val box = Box(
                        user.pos.x - 5.0, user.pos.y - 5.0, user.pos.z - 5.0,
                        user.pos.x + 5.0, user.pos.y + 5.0, user.pos.z + 5.0
                    )
                    
                    val entities = world.getEntitiesByClass(LivingEntity::class.java, box) { entity ->
                        entity != user // 包括所有生物实体，而不只是敌对生物
                    }
                    
                    if (entities.isNotEmpty()) {
                        // 播放音效
                        world.playSound(
                            null,
                            user.x, user.y, user.z,
                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                        )
                        
                        // 向玩家发送使用成功的消息
                        user.sendMessage(Text.translatable("message.blasphemy.seraphim_sword.use").formatted(Formatting.GOLD), true)
                        
                        // 对每个敌人应用效果
                        for (entity in entities) {
                            // 计算推力方向
                            val pushDirection = entity.pos.subtract(user.pos).normalize().multiply(config.seraphimSword.pushStrength.toDouble())
                            
                            // 应用推力
                            entity.addVelocity(pushDirection.x, 0.5, pushDirection.z)
                            entity.velocityDirty = true
                            
                            // 造成固定伤害加上配置中设定比例的当前生命值的伤害
                            val healthDamage = entity.health * config.seraphimSword.specialDamageMultiplier
                            val fixedDamage = 2.0f // 增加2点固定伤害
                            entity.damage(user.damageSources.playerAttack(user), healthDamage + fixedDamage)
                            
                            // 发送粒子效果给客户端
                            if (config.seraphimSword.enableParticles) {
                                // 闪光效果
                                world.addParticle(
                                    ParticleTypes.FLASH,
                                    entity.x,
                                    entity.y + 1.0,
                                    entity.z,
                                    0.0, 0.0, 0.0
                                )
                                
                                // 服务端发送给客户端的圆形冲击效果
                                for (i in 0 until 16) {
                                    val angle = (Math.PI * 2.0 * i) / 16
                                    val distance = 0.7
                                    val x = entity.x + cos(angle) * distance
                                    val z = entity.z + sin(angle) * distance
                                    
                                    // 使用服务端方法发送粒子效果到客户端
                                    ((world) as ServerWorld).spawnParticles(
                                        ParticleTypes.END_ROD,
                                        x,
                                        entity.y + 1.0,
                                        z,
                                        1,
                                        0.0, 0.0, 0.0,
                                        0.0
                                    )
                                }
                            }
                        }
                        
                        // 设置冷却时间
                        cooldowns[user.uuid] = currentTime
                        return TypedActionResult.success(stack)
                    }
                }
            } else {
                // 冷却中
                if (!world.isClient) {
                    val remainingCooldown = (seraphimCooldown - (currentTime - lastUseTime)) / 1000
                    // 向玩家发送冷却消息
                    user.sendMessage(
                        Text.translatable(
                            "message.blasphemy.seraphim_sword.cooldown",
                            remainingCooldown
                        ).formatted(Formatting.RED),
                        true
                    )
                }
            }
        }
        
        return super.use(world, user, hand)
    }

    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        
        if (entity is PlayerEntity && selected) {
            // 天使剑特性：白天回复耐久和防火效果
            if (swordType == SwordType.SERAPHIM) {
                // 免疫火焰并熄灭身上的火焰
                if (entity.isOnFire) {
                    entity.fireTicks = 0
                }
                
                // 白天时回复耐久
                if (world.isDay && stack.damage > 0 && world.random.nextFloat() < 0.01f) {
                    stack.damage = stack.damage - 1
                }
            }
        }
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (attacker is PlayerEntity) {
            when (swordType) {
                SwordType.SERAPHIM -> {
                    // 对亡灵伤害增加配置中设定的百分比
                    if (isUndead(target)) {
                        // 计算基础攻击伤害
                        val baseAttackDamage = getMaterial().attackDamage + config.seraphimSword.baseDamage
                        
                        // 计算亡灵额外伤害
                        val undeadBonusDamage = baseAttackDamage * config.seraphimSword.undeadDamageMultiplier
                        
                        // 应用额外伤害 - 直接造成伤害而不经过护甲等减免
                        target.damage(attacker.world.damageSources.magic(), undeadBonusDamage)
                    }
                    
                    // 应用魂火效果（莱兰特模组兼容）
                    LeylinesCompat.applySoulFireEffect(target, 5, 60) // 魂火V，持续3秒(60刻)
                }
                SwordType.RAPIDS -> {
                    // 攻击单体造成额外伤害
                    target.damage(attacker.damageSources.playerAttack(attacker), 
                                target.maxHealth * config.rapidsSword.specialDamageMultiplier)
                    
                    // 概率斩杀血量低于阈值的目标
                    if (target.health <= target.maxHealth * config.rapidsSword.executionThreshold && 
                        Random.nextFloat() < config.rapidsSword.executionChance) {
                        target.damage(attacker.damageSources.playerAttack(attacker), Float.MAX_VALUE)
                    }
                }
                SwordType.MURAMASA -> {
                    // 概率斩杀血量低于阈值的目标
                    if (target.health <= target.maxHealth * config.muramasaSword.executionThreshold && 
                        Random.nextFloat() < config.muramasaSword.executionChance) {
                        target.damage(attacker.damageSources.playerAttack(attacker), Float.MAX_VALUE)
                    }
                    
                    // 对护甲超过阈值的生物造成额外伤害
                    if (target.armor > config.muramasaSword.armorThreshold) {
                        target.damage(attacker.damageSources.playerAttack(attacker), 
                                    target.maxHealth * config.muramasaSword.specialDamageMultiplier)
                    }
                }
            }
        }
        
        return super.postHit(stack, target, attacker)
    }
    
    /**
     * 检查实体是否为亡灵
     */
    private fun isUndead(entity: LivingEntity): Boolean {
        // 1. 首先检查实体标签
        for (tagString in config.seraphimSword.undeadEntityTags) {
            try {
                val tagId = Identifier.tryParse(tagString)
                if (tagId != null) {
                    val tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId)
                    if (entity.type.isIn(tagKey)) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // 忽略无效的标签
                Blasphemy.logger.warn("无效的实体标签: $tagString")
            }
        }
        
        // 2. 然后检查特定实体类型（兼容不使用标签的情况）
        return false
    }
} 