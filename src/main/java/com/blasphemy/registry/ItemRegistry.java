package com.blasphemy.registry;

import com.blasphemy.Blasphemy;
import com.blasphemy.config.ModConfig;
import com.blasphemy.items.MuramasaSword;
import com.blasphemy.items.RapidsSword;
import com.blasphemy.items.SeraphimSword;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * 物品注册表类
 * 负责注册所有模组物品
 */
public class ItemRegistry {
    // 定义物品实例
    public static final Item SERAPHIM_SWORD = new SeraphimSword(
            ToolMaterials.NETHERITE,
            (int) ModConfig.getConfig().seraphimSword.baseDamage,
            ModConfig.getConfig().seraphimSword.attackSpeed,
            new FabricItemSettings().rarity(Rarity.EPIC).fireproof()
    );
    
    public static final Item RAPIDS_SWORD = new RapidsSword(
            ToolMaterials.DIAMOND,
            (int) ModConfig.getConfig().rapidsSword.baseDamage,
            ModConfig.getConfig().rapidsSword.attackSpeed,
            new FabricItemSettings().rarity(Rarity.RARE)
    );
    
    public static final Item MURAMASA_SWORD = new MuramasaSword(
            ToolMaterials.NETHERITE,
            (int) ModConfig.getConfig().muramasaSword.baseDamage,
            ModConfig.getConfig().muramasaSword.attackSpeed,
            new FabricItemSettings().rarity(Rarity.EPIC).fireproof()
    );
    
    /**
     * 注册所有物品
     */
    public static void register() {
        Blasphemy.LOGGER.info("正在注册物品...");
        
        // 注册武器
        registerItem("seraphim_sword", SERAPHIM_SWORD);
        registerItem("rapids_sword", RAPIDS_SWORD);
        registerItem("muramasa_sword", MURAMASA_SWORD);
        
        // 将物品添加到物品组
        addItemsToItemGroup();
        
        Blasphemy.LOGGER.info("物品注册完成！");
    }
    
    /**
     * 将物品添加到物品组
     */
    private static void addItemsToItemGroup() {
        // 添加到模组物品组
        ItemGroupEvents.modifyEntriesEvent(ItemGroupRegistry.BLASPHEMY_GROUP).register(entries -> {
            entries.add(SERAPHIM_SWORD);
            entries.add(RAPIDS_SWORD);
            entries.add(MURAMASA_SWORD);
        });
    }
    
    /**
     * 注册单个物品的辅助方法
     */
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Blasphemy.MOD_ID, name), item);
    }
} 