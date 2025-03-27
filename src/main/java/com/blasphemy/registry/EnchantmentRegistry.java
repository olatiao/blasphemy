package com.blasphemy.registry;

import com.blasphemy.Blasphemy;
import com.blasphemy.enchantment.CleaveEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 附魔注册表类
 * 负责注册所有自定义附魔
 */
public class EnchantmentRegistry {
    
    // 群体斩击附魔
    public static final Enchantment CLEAVE = new CleaveEnchantment();
    
    /**
     * 注册所有附魔
     */
    public static void register() {
        Blasphemy.LOGGER.info("正在注册附魔...");
        
        // 注册群体斩击附魔
        registerEnchantment("cleave", CLEAVE);

        Blasphemy.LOGGER.info("附魔注册完成！");
    }
    
    /**
     * 注册单个附魔的辅助方法
     */
    private static Enchantment registerEnchantment(String name, Enchantment enchantment) {
        return Registry.register(Registries.ENCHANTMENT, new Identifier(Blasphemy.MOD_ID, name), enchantment);
    }
} 