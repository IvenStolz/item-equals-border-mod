package itemequalsborder.modid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KnownItemsData extends SavedData {

    private final Set<Item> knownItems;
    private double borderSize;
    private boolean netherReached;
    private double netherBorderCenterX;
    private double netherBorderCenterZ;

    public KnownItemsData() {
        this(new HashSet<>(), 1.0, false, 0.5, 0.5);
    }

    private KnownItemsData(Set<Item> knownItems, double borderSize, boolean netherReached, double netherBorderCenterX, double netherBorderCenterZ) {
        this.knownItems = knownItems;
        this.borderSize = borderSize;
        this.netherReached = netherReached;
        this.netherBorderCenterX = netherBorderCenterX;
        this.netherBorderCenterZ = netherBorderCenterZ;
    }

    // true = das Item war neu
    public boolean registerIfNew(Item item) {
        boolean wasNew = knownItems.add(item);
        if (wasNew) {
            setDirty();
        }
        return wasNew;
    }

    private Set<Item> getKnownItems() {
        return knownItems;
    }

    public double getBorderSize() {
        return borderSize;
    }

    public Set<Item> getKnownItemsView() {
        return java.util.Collections.unmodifiableSet(knownItems);
    }

    public void setBorderSize(double size) {
        this.borderSize = size;
        setDirty();
    }

    public boolean hasReachedNether() {
        return netherReached;
    }

    public double getNetherBorderCenterX() {
        return netherBorderCenterX;
    }

    public double getNetherBorderCenterZ() {
        return netherBorderCenterZ;
    }

    public boolean setNetherBorderIfFirstEntry(double x, double z) {
        if (netherReached) {return false;}
        this.netherReached = true;
        this.netherBorderCenterX = x;
        this.netherBorderCenterZ = z;
        setDirty();
        return true;
    }

    private static final Codec<Set<Item>> ITEM_SET_CODEC =
            BuiltInRegistries.ITEM.byNameCodec()
                    .listOf()
                    .xmap(HashSet::new, List::copyOf);

    private static final Codec<KnownItemsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_SET_CODEC.fieldOf("known_items").forGetter(KnownItemsData::getKnownItems),
            Codec.DOUBLE.fieldOf("border_size").forGetter(KnownItemsData::getBorderSize),
            Codec.BOOL.optionalFieldOf("nether_reached", false).forGetter(KnownItemsData::hasReachedNether),
            Codec.DOUBLE.optionalFieldOf("center_x", 0.5).forGetter(KnownItemsData::getNetherBorderCenterX),
            Codec.DOUBLE.optionalFieldOf("center_z", 0.5).forGetter(KnownItemsData::getNetherBorderCenterZ)
    ).apply(instance, KnownItemsData::new));

    public static final SavedDataType<KnownItemsData> TYPE = new SavedDataType<>(
            ItemEqualsBorder.id("known_items"),
            KnownItemsData::new,
            CODEC,
            DataFixTypes.LEVEL
    );
}