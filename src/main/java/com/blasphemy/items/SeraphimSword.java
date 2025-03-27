package com.blasphemy.items;

import com.blasphemy.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 炽天使武器类
 * 对亡灵生物造成额外伤害，具有群体推退和伤害技能
 */
public class SeraphimSword extends BaseSword {
    // 技能冷却时间记录
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    // 中毒效果的持续时间(秒)
    private static final int EFFECT_DURATION = 3;
    // 中毒效果的强度(0=I, 4=V)
    private static final int EFFECT_AMPLIFIER = 4;
    // 中毒效果的id
    private static final String EFFECT_ID = "l2complements.flame";

    public SeraphimSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings, "seraphim_sword");
    }

    /**
     * 为目标添加指定效果
     * 
     * @param target    目标实体
     * @param effectId  效果id
     * @param duration  持续时间(秒)
     * @param amplifier 效果强度(0-4)
     * @return 是否成功添加效果
     */
    private void applyEffectById(LivingEntity target, String effectId, int duration, int amplifier) {
        // 检查目标是否有效
        if (target == null || target.getWorld().isClient
                || target.isInvulnerableTo(target.getDamageSources().magic())) {
            return;
        }

        // 从注册表获取效果
        Identifier id = new Identifier(effectId);
        StatusEffect effect = Registries.STATUS_EFFECT.get(id);
        if (effect == null) {
            return;
        }

        // 为目标添加效果
        target.addStatusEffect(
                new StatusEffectInstance(
                        effect,
                        duration * 20,
                        amplifier,
                        false,
                        true,
                        true));
    }

    /**
     * 对亡灵生物造成额外伤害
     * 
     * @param target     目标实体
     * @param attacker   攻击者
     * @param baseDamage 基础伤害
     */
    private void applyExtraDamage(LivingEntity target, LivingEntity attacker, float baseDamage) {
        // 检查目标是否有效
        if (target == null || target.getWorld().isClient
                || target.isInvulnerableTo(target.getDamageSources().magic())) {
            return;
        }
        // 对亡灵生物造成额外伤害
        if (target.getGroup() == EntityGroup.UNDEAD) {
            float extraDamage = ModConfig.getConfig().seraphimSword.baseDamage
                    * ModConfig.getConfig().seraphimSword.undeadDamageMultiplier;
            target.damage(target.getDamageSources().generic(), extraDamage);

            // 播放特效
            if (attacker instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("message.blasphemy.seraphim_sword.undead_bonus"), true);
            }

            // 播放音效与粒子效果
            target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_ZOMBIE_HURT, SoundCategory.PLAYERS, 1.0f, 1.5f);

            if (target.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        15, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 基础伤害处理
        boolean result = super.postHit(stack, target, attacker);

        // 为目标添加效果
        applyEffectById(target, EFFECT_ID, EFFECT_DURATION, EFFECT_AMPLIFIER);

        // 对亡灵生物造成额外伤害
        applyExtraDamage(target, attacker, ModConfig.getConfig().seraphimSword.baseDamage);

        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // 检查冷却时间
        UUID playerUuid = player.getUuid();
        long currentTime = world.getTime();
        long cooldownTicks = ModConfig.getConfig().seraphimSword.cooldownSeconds * 20; // 转换为游戏刻

        if (cooldowns.containsKey(playerUuid)) {
            long lastUsed = cooldowns.get(playerUuid);
            long remainingTicks = lastUsed + cooldownTicks - currentTime;

            if (remainingTicks > 0) {
                // 冷却中，通知玩家
                if (!world.isClient) {
                    int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
                    player.sendMessage(
                            Text.translatable("message.blasphemy.seraphim_sword.cooldown", remainingSeconds)
                                    .formatted(Formatting.RED),
                            true);
                }
                return TypedActionResult.fail(stack);
            }
        }

        // 使用技能
        if (!world.isClient) {
            // 更新冷却时间
            cooldowns.put(playerUuid, currentTime);

            // 效果范围
            double radius = 5.0;
            float pushStrength = ModConfig.getConfig().seraphimSword.pushStrength;
            float damage = ModConfig.getConfig().seraphimSword.baseDamage
                    * ModConfig.getConfig().seraphimSword.specialDamageMultiplier;

            // 获取范围内的所有生物
            List<Entity> entities = world.getOtherEntities(player,
                    new Box(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                            player.getX() + radius, player.getY() + radius, player.getZ() + radius),
                    entity -> entity instanceof LivingEntity && entity != player);

            if (!entities.isEmpty()) {
                // 播放音效
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);

                // 对每个实体应用效果
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity livingEntity) {
                        // 伤害
                        livingEntity.damage(livingEntity.getDamageSources().playerAttack(player), damage);

                        // 击退效果
                        Vec3d pushDirection = livingEntity.getPos().subtract(player.getPos()).normalize();
                        livingEntity.setVelocity(pushDirection.x * pushStrength,
                                0.5,
                                pushDirection.z * pushStrength);
                        livingEntity.velocityModified = true;
                    }
                }

                // 粒子效果
                if (ModConfig.getConfig().seraphimSword.enableParticles) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    for (int i = 0; i < ModConfig.getConfig().seraphimSword.particleCount; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double distance = Math.random() * radius;
                        double x = player.getX() + Math.cos(angle) * distance;
                        double z = player.getZ() + Math.sin(angle) * distance;

                        serverWorld.spawnParticles(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                x, player.getY() + 0.5, z,
                                1, 0, 0, 0, 0.05);
                    }
                }

                // 通知玩家
                player.sendMessage(Text.translatable("message.blasphemy.seraphim_sword.use")
                        .formatted(Formatting.YELLOW), true);
            }
        }

        // 触发冷却动画
        player.getItemCooldownManager().set(this, 20); // 短暂的操作冷却

        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        // 在白天自动修复
        if (!world.isClient && entity instanceof PlayerEntity && world.isDay() && world.random.nextFloat() < 0.01f) {
            if (stack.getDamage() > 0) {
                stack.setDamage(stack.getDamage() - 1);
            }
        }
    }

    @Override
    public boolean isDamageable() {
        return true; // 可以受到损耗
    }

    @Override
    public boolean isFireproof() {
        return true; // 免疫火焰
    }
}