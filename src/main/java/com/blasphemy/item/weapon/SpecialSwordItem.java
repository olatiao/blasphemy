package com.blasphemy.item.weapon;

import com.blasphemy.config.ModConfig;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 特殊剑武器类，包含特殊能力的剑
 */
public class SpecialSwordItem extends MagicSwordItem {
    // 剑类型
    public enum SwordType {
        SERAPHIM,   // 天使剑
        RAPIDS,     // 急流剑
        MURAMASA    // 村正剑
    }

    private final SwordType swordType;
    
    // 用于存储每个玩家使用技能的冷却时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * 构造函数
     */
    public SpecialSwordItem(
        ToolMaterial material,
        int attackDamage,
        float attackSpeed,
        Settings settings,
        SwordType swordType
    ) {
        super(material, attackDamage, attackSpeed, settings);
        this.swordType = swordType;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // 普通的交互处理
        return super.useOnEntity(stack, user, entity, hand);
    }

    @Override
    public void appendTooltip(
        ItemStack stack,
        World world,
        List<Text> tooltip,
        TooltipContext context
    ) {
        super.appendTooltip(stack, world, tooltip, context);
        
        // 添加武器特殊效果提示
        String tooltipKey = "item.blasphemy." + getTooltipIdBySwordType() + ".tooltip";
        tooltip.add(Text.translatable(tooltipKey));
    }
    
    /**
     * 根据剑类型获取提示ID
     */
    private String getTooltipIdBySwordType() {
        return switch (swordType) {
            case SERAPHIM -> "seraphim_sword";
            case RAPIDS -> "rapids_sword";
            case MURAMASA -> "muramasa_sword";
        };
    }

    /**
     * 获取天使剑的冷却时间（毫秒）
     */
    private long getSeraphimCooldown() {
        return ModConfig.getConfig().seraphimSword.cooldownSeconds * 1000L;
    }

    /**
     * 创建粒子光环效果
     */
    private void createParticleAura(World world, PlayerEntity user) {
        if (!ModConfig.getConfig().seraphimSword.enableParticles) return;
        
        // 生成环绕光环粒子效果
        double radius = 2.0;
        int particlesPerRing = 20;
        int rings = 3;
        
        // 添加闪光中心效果
        world.addParticle(
            ParticleTypes.FLASH,
            user.getX(),
            user.getY() + 1.0,
            user.getZ(),
            0.0,
            0.0,
            0.0
        );
        
        // 创建粒子光环
        for (int ring = 0; ring < rings; ring++) {
            double ringHeight = 0.2 * ring;
            double ringRadius = radius - (ring * 0.5);
            
            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (Math.PI * 2.0 * i) / particlesPerRing;
                double x = user.getX() + Math.cos(angle) * ringRadius;
                double z = user.getZ() + Math.sin(angle) * ringRadius;
                
                world.addParticle(
                    ParticleTypes.END_ROD,
                    x,
                    user.getY() + 1.0 + ringHeight,
                    z,
                    0.0,
                    0.05,
                    0.0
                );
            }
        }
        
        // 添加随机扩散粒子
        for (int i = 0; i < ModConfig.getConfig().seraphimSword.particleCount; i++) {
            world.addParticle(
                ParticleTypes.END_ROD,
                user.getX() + (world.getRandom().nextDouble() - 0.5) * 2,
                user.getY() + 1.0,
                user.getZ() + (world.getRandom().nextDouble() - 0.5) * 2,
                (world.getRandom().nextDouble() - 0.5) * 0.2,
                world.getRandom().nextDouble() * 0.2,
                (world.getRandom().nextDouble() - 0.5) * 0.2
            );
        }
        
        // 添加额外的光束效果
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2.0 * i) / 8;
            double distance = 5.0;
            double x = user.getX() + Math.cos(angle) * distance;
            double z = user.getZ() + Math.sin(angle) * distance;
            
            world.addParticle(
                ParticleTypes.SOUL_FIRE_FLAME,
                x,
                user.getY(),
                z,
                0.0,
                0.05,
                0.0
            );
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // 天使剑技能：推开敌人并造成伤害
        if (swordType == SwordType.SERAPHIM) {
            long currentTime = System.currentTimeMillis();
            long lastUseTime = cooldowns.getOrDefault(user.getUuid(), 0L);
            
            // 检查冷却时间
            if (currentTime - lastUseTime >= getSeraphimCooldown()) {
                // 客户端粒子效果
                if (world.isClient()) {
                    createParticleAura(world, user);
                }
                
                if (!world.isClient()) {
                    // 获取5格范围内的敌对生物
                    Box box = new Box(
                        user.getPos().x - 5.0, user.getPos().y - 5.0, user.getPos().z - 5.0,
                        user.getPos().x + 5.0, user.getPos().y + 5.0, user.getPos().z + 5.0
                    );
                    
                    List<LivingEntity> entities = world.getEntitiesByClass(
                        LivingEntity.class, 
                        box, 
                        entity -> entity != user // 包括所有生物实体，而不只是敌对生物
                    );
                    
                    if (!entities.isEmpty()) {
                        // 播放音效
                        world.playSound(
                            null,
                            user.getX(), user.getY(), user.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                        );
                        
                        // 向玩家发送使用成功的消息
                        user.sendMessage(
                            Text.translatable("message.blasphemy.seraphim_sword.use")
                                .formatted(Formatting.GOLD), 
                            true
                        );
                        
                        // 对每个敌人应用效果
                        for (LivingEntity entity : entities) {
                            // 计算推力方向
                            double pushStrength = ModConfig.getConfig().seraphimSword.pushStrength;
                            double dx = entity.getX() - user.getX();
                            double dz = entity.getZ() - user.getZ();
                            double distance = Math.sqrt(dx * dx + dz * dz);
                            
                            // 避免除以零
                            if (distance < 0.1) {
                                dx = 0;
                                dz = 1.0;
                                distance = 1.0;
                            }
                            
                            // 计算单位向量
                            double nx = dx / distance;
                            double nz = dz / distance;
                            
                            // 应用推力
                            entity.addVelocity(
                                nx * pushStrength, 
                                0.5, 
                                nz * pushStrength
                            );
                            entity.velocityModified = true;
                            
                            // 造成固定伤害加上配置中设定比例的当前生命值的伤害
                            float healthDamage = entity.getHealth() * ModConfig.getConfig().seraphimSword.specialDamageMultiplier;
                            float fixedDamage = 2.0f; // 增加2点固定伤害
                            entity.damage(user.getDamageSources().playerAttack(user), healthDamage + fixedDamage);
                            
                            // 发送粒子效果给客户端
                            if (ModConfig.getConfig().seraphimSword.enableParticles) {
                                // 使用服务端方法发送粒子效果到客户端
                                ((ServerWorld) world).spawnParticles(
                                    ParticleTypes.FLASH,
                                    entity.getX(),
                                    entity.getY() + 1.0,
                                    entity.getZ(),
                                    1,
                                    0.0, 0.0, 0.0,
                                    0.0
                                );
                                
                                // 服务端发送给客户端的圆形冲击效果
                                for (int i = 0; i < 16; i++) {
                                    double angle = (Math.PI * 2.0 * i) / 16;
                                    double particleDistance = 0.7;
                                    double x = entity.getX() + Math.cos(angle) * particleDistance;
                                    double z = entity.getZ() + Math.sin(angle) * particleDistance;
                                    
                                    ((ServerWorld) world).spawnParticles(
                                        ParticleTypes.END_ROD,
                                        x,
                                        entity.getY() + 1.0,
                                        z,
                                        1,
                                        0.0, 0.0, 0.0,
                                        0.0
                                    );
                                }
                            }
                        }
                        
                        // 设置冷却时间
                        cooldowns.put(user.getUuid(), currentTime);
                        return TypedActionResult.success(stack);
                    }
                }
            } else if (world.isClient()) {
                // 显示冷却中消息
                long remainingCooldown = (getSeraphimCooldown() - (currentTime - lastUseTime)) / 1000;
                user.sendMessage(
                    Text.translatable("message.blasphemy.seraphim_sword.cooldown", remainingCooldown)
                        .formatted(Formatting.RED),
                    true
                );
                return TypedActionResult.fail(stack);
            }
        }
        
        // 其他类型剑的技能...
        
        return TypedActionResult.pass(stack);
    }
} 