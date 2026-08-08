package itemequalsborder.modid.client;

import net.minecraft.world.item.Item;

import java.util.List;

public class ClientKnownItemsCache {
    private static List<Item> items = List.of();
    private static double borderSize = 1.0;

    public static void update(List<Item> newItems, double newBorderSize) {
        items = newItems;
        borderSize = newBorderSize;
    }

    public static List<Item> getItems() {
        return items;
    }

    public static double getBorderSize() {
        return borderSize;
    }
}