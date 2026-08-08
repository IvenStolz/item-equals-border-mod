package itemequalsborder.modid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ItemPoints {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("item-equals-border.json");

    private static ItemPointsConfig config;
    private static final Map<Item, Integer> resolvedItemPoints = new LinkedHashMap<>();
    private static final Map<TagKey<Item>, Integer> resolvedTagPoints = new LinkedHashMap<>();

    public static void load() {
        try {
            // Wenn die Config beim Spieler noch nicht existiert
            if (!Files.exists(CONFIG_PATH)) {
                Files.createDirectories(CONFIG_PATH.getParent());

                // Versuche, das große JSON aus deinen Mod-Ressourcen zu kopieren
                try (java.io.InputStream in = ItemPoints.class.getResourceAsStream("/item-equals-border.json")) {
                    if (in != null) {
                        Files.copy(in, CONFIG_PATH);
                    } else {
                        // Fallback, falls die JSON-Datei im resources-Ordner fehlt
                        config = ItemPointsConfig.createDefault();
                        save();
                    }
                }
            }

            // Normale Lade-Logik (liest die nun vorhandene Datei ein)
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    config = GSON.fromJson(reader, ItemPointsConfig.class);
                }
            }
        } catch (IOException e) {
            ItemEqualsBorder.LOGGER.error("Konnte item-equals-border.json nicht laden, nutze Defaults", e);
            config = ItemPointsConfig.createDefault();
        }

        resolveEntries();
    }

    private static void save() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(config, writer);
        }
    }

    private static void resolveEntries() {
        resolvedItemPoints.clear();
        resolvedTagPoints.clear();

        for (Map.Entry<String, Integer> entry : config.itemPoints.entrySet()) {
            Identifier id = Identifier.parse(entry.getKey());
            Optional<Item> item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id);
            if (item.isPresent()) {
                resolvedItemPoints.put(item.get(), entry.getValue());
            } else {
                ItemEqualsBorder.LOGGER.warn("Unbekanntes Item in item-equals-border.json: {}", entry.getKey());
            }
        }

        for (Map.Entry<String, Integer> entry : config.tagPoints.entrySet()) {
            Identifier id = Identifier.parse(entry.getKey());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
            resolvedTagPoints.put(tag, entry.getValue());
        }
    }

    public static int getPoints(Item item) {
        Integer direct = resolvedItemPoints.get(item);
        if (direct != null) {
            return direct;
        }

        for (Map.Entry<TagKey<Item>, Integer> tier : resolvedTagPoints.entrySet()) {
            if (item.builtInRegistryHolder().is(tier.getKey())) {
                return tier.getValue();
            }
        }

        return config.defaultPoints;
    }

    public static int getColor(int points) {
        if (points >= 50) return 0xFFFF0000;
        if (points >= 20) return 0xFFFFAA00;
        if (points >= 10) return 0xFFFF00FF;
        if (points >= 5) return 0xFF55FFFF;
        if (points >= 2) return 0xFF55FF55;
        return 0xFFAAAAAA;
    }
}