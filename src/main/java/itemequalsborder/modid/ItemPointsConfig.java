package itemequalsborder.modid;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemPointsConfig {
    public int defaultPoints = 1;

    // Item-ID (z.B. "minecraft:diamond") -> Punkte
    public Map<String, Integer> itemPoints = new LinkedHashMap<>();

    // Tag-ID (z.B. "minecraft:swords") -> Punkte
    public Map<String, Integer> tagPoints = new LinkedHashMap<>();

    public static ItemPointsConfig createDefault() {
        ItemPointsConfig config = new ItemPointsConfig();
        config.itemPoints.put("minecraft:netherite_ingot", 10);
        config.itemPoints.put("minecraft:elytra", 10);
        config.itemPoints.put("minecraft:diamond", 5);
        config.itemPoints.put("minecraft:nether_star", 5);
        config.tagPoints.put("minecraft:swords", 3);
        config.tagPoints.put("minecraft:logs", 1);
        return config;
    }
}