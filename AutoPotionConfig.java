package cz.autopotion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AutoPotionConfig {

    public static boolean enabled = true;
    public static int tickDelay = 20; // každou sekundu zkontroluj (20 ticků = 1s)
    public static List<PotionRule> rules = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("autopotion.json");

    public static class PotionRule {
        public String potionType;   // např. "healing", "regeneration", "strength"
        public String displayName;  // Zobrazovaný název v GUI
        public boolean enabled;
        public float threshold;     // Procento HP, pod které se hodí (0-100)
        public int priority;        // Nižší číslo = vyšší priorita

        public PotionRule(String potionType, String displayName, boolean enabled, float threshold, int priority) {
            this.potionType = potionType;
            this.displayName = displayName;
            this.enabled = enabled;
            this.threshold = threshold;
            this.priority = priority;
        }
    }

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    enabled = data.enabled;
                    tickDelay = data.tickDelay;
                    if (data.rules != null && !data.rules.isEmpty()) {
                        rules = data.rules;
                        return;
                    }
                }
            } catch (IOException e) {
                AutoPotion.LOGGER.error("Chyba při načítání configu: " + e.getMessage());
            }
        }
        // Výchozí pravidla
        setDefaults();
        save();
    }

    public static void setDefaults() {
        rules.clear();
        rules.add(new PotionRule("healing",      "💉 Lektvar léčení",        true,  50f, 1));
        rules.add(new PotionRule("regeneration", "❤️ Lektvar regenerace",    false, 70f, 2));
        rules.add(new PotionRule("strength",     "💪 Lektvar síly",          false, 100f, 3));
        rules.add(new PotionRule("fire_resistance","🔥 Ohnivzdorný lektvar", false, 100f, 4));
        rules.add(new PotionRule("speed",        "⚡ Lektvar rychlosti",     false, 100f, 5));
        rules.add(new PotionRule("invisibility", "👻 Lektvar neviditelnosti",false, 100f, 6));
        rules.add(new PotionRule("water_breathing","💧 Dýchání pod vodou",   false, 100f, 7));
        rules.add(new PotionRule("night_vision", "👁️ Noční vidění",         false, 100f, 8));
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            ConfigData data = new ConfigData();
            data.enabled = enabled;
            data.tickDelay = tickDelay;
            data.rules = rules;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            AutoPotion.LOGGER.error("Chyba při ukládání configu: " + e.getMessage());
        }
    }

    private static class ConfigData {
        boolean enabled = true;
        int tickDelay = 20;
        List<PotionRule> rules = new ArrayList<>();
    }
}
