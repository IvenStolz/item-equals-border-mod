package itemequalsborder.modid;

import net.minecraft.network.chat.Component;

public final class Lang {
    private Lang() {}

    public static Component tr(String key, Object... args) {
        return Component.translatable(ItemEqualsBorder.MOD_ID + "." + key, args);
    }

    public static String str(String key, Object... args) {
        return tr(key, args).getString();
    }

    public static Component points(int amount) {
        return tr(amount == 1 ? "points.one" : "points.other", amount);
    }
}