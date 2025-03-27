package com.blasphemy.config;

import com.blasphemy.Blasphemy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 模组配置类
 * 负责加载和保存模组配置
 */
public class ModConfig {
    
    private static ModConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("blasphemy.json").toFile();
    
    // 炽天使配置
    public SeraphimSwordConfig seraphimSword = new SeraphimSwordConfig();
    
    // 激流之剑配置
    public RapidsSwordConfig rapidsSword = new RapidsSwordConfig();
    
    // 妖刀村正配置
    public MuramasaSwordConfig muramasaSword = new MuramasaSwordConfig();
    
    // 群体斩击附魔配置
    public CleaveConfig cleaveConfig = new CleaveConfig();
    
    // 传送门配置
    public PortalConfig portalConfig = new PortalConfig();
    
    /**
     * 加载配置
     */
    public static void load() {
        Blasphemy.LOGGER.info("加载配置文件...");
        
        // 如果配置文件不存在，创建默认配置
        if (!CONFIG_FILE.exists()) {
            instance = new ModConfig();
            save();
            Blasphemy.LOGGER.info("创建默认配置文件");
            return;
        }
        
        // 读取配置文件
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            instance = GSON.fromJson(reader, ModConfig.class);
            Blasphemy.LOGGER.info("成功加载配置文件");
        } catch (IOException e) {
            Blasphemy.LOGGER.error("加载配置文件失败", e);
            instance = new ModConfig();
        }
    }
    
    /**
     * 保存配置
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
            Blasphemy.LOGGER.info("成功保存配置文件");
        } catch (IOException e) {
            Blasphemy.LOGGER.error("保存配置文件失败", e);
        }
    }
    
    /**
     * 获取配置实例
     */
    public static ModConfig getConfig() {
        if (instance == null) {
            load();
        }
        return instance;
    }
    
    /**
     * 炽天使配置类
     */
    public static class SeraphimSwordConfig {
        public float baseDamage = 7.0f;
        public float attackSpeed = -2.4f;
        public float specialDamageMultiplier = 0.1f;
        public float pushStrength = 1.5f;
        public int cooldownSeconds = 60;
        public int particleCount = 40;
        public boolean enableParticles = true;
        public float undeadDamageMultiplier = 0.3f;
        public List<String> undeadEntityTags = Arrays.asList("minecraft:undead", "forge:undead", "c:undead");
    }
    
    /**
     * 激流之剑配置类
     */
    public static class RapidsSwordConfig {
        public float baseDamage = 6.0f;
        public float attackSpeed = -1.9f;
        public float specialDamageMultiplier = 0.05f;
        public float executionThreshold = 0.2f;
        public float executionChance = 0.3f;
    }
    
    /**
     * 妖刀村正配置类
     */
    public static class MuramasaSwordConfig {
        public float baseDamage = 8.0f;
        public float attackSpeed = -2.6f;
        public float specialDamageMultiplier = 0.025f;
        public float executionThreshold = 0.2f;
        public float executionChance = 0.3f;
        public float armorThreshold = 10.0f;
    }
    
    /**
     * 群体斩击附魔配置类
     */
    public static class CleaveConfig {
        public float attackRange = 3.0f;
        public float baseMultiplier = 0.15f;
        public float levelMultiplier = 0.05f;
        public int maxLevel = 5;
    }
    
    /**
     * 传送门配置类
     */
    public static class PortalConfig {
        public boolean enabled = true;
        public boolean supportVanillaItems = true;
        public List<String> portalBlocks = Arrays.asList("minecraft:crying_obsidian", "minecraft:diamond_block");
        public String ignitionItem = "minecraft:heart_of_the_sea";
        public PortalMessages messages = new PortalMessages();
        
        public static class PortalMessages {
            public String invalidBlock = "message.blasphemy.portal.invalid_block";
            public String invalidItem = "message.blasphemy.portal.invalid_item";
        }
    }
}