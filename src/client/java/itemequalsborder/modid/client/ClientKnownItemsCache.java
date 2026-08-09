package itemequalsborder.modid.client;

import net.minecraft.world.item.Item;

import java.util.List;

public class ClientKnownItemsCache {
    private static List<Item> items = List.of();
    private static List<String> discoverers = List.of();
    private static double borderSize = 1.0;

    public static void update(List<Item> newItems, List<String> newDiscoverers, double newBorderSize) {
        items = newItems;
        discoverers = newDiscoverers;
        borderSize = newBorderSize;
    }

    public static List<Item> getItems() { return items; }
    public static double getBorderSize() { return borderSize; }

    public static String getDiscoverer(Item item) {
        int idx = items.indexOf(item);
        return idx >= 0 ? discoverers.get(idx) : "Unbekannt";
    }

    public static List<String> getDiscoverersList() {
        return discoverers;
    }
}