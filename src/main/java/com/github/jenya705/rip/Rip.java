package com.github.jenya705.rip;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class Rip extends JavaPlugin {

    private static Rip instance;
    public static Rip getInstance() {
        return instance;
    }

    private RipConfig config;
    private RipDatabase database;
    @Getter
    private final Map<Location, Inventory> inventories = new HashMap<>();

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        config = new RipConfig(getConfig());
        database = new RipDatabase();
        getServer().getPluginManager().registerEvents(new RipHandler(), this);
        addRecipe();
    }

    @Override
    public void onDisable() {
        inventories.forEach((location, inventory) -> location.getBlock().setType(Material.AIR));
    }

    public RipConfig config() {
        return config;
    }

    public RipDatabase database() {
        return database;
    }

    private void addRecipe() {
        ItemStack key = new ItemStack(Material.STICK);
        ItemMeta meta = key.getItemMeta();
        meta.setCustomModelData(config.getCustomModelData());
        key.setItemMeta(meta);
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(this, "key"), key);
        recipe.addIngredient(Material.TOTEM_OF_UNDYING);
        recipe.addIngredient(Material.EMERALD);
        getServer().addRecipe(recipe);
    }

}
