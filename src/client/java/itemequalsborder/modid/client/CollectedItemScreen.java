package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CollectedItemScreen extends Screen {
    public Screen parent;
    private static final int GRID_MARGIN_LEFT = 200;
    private static final int GRID_MARGIN_RIGHT = 200;

    public CollectedItemScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button closeButton = Button.builder(Component.literal("X"), button -> onClose()).bounds(this.width - 25, 5, 20, 20).build();
        this.addRenderableWidget(closeButton);

        Button sortButton = Button.builder(
                Component.literal("Sortierung: " + sortLabel()),
                button -> {
                    sortMode = (sortMode == SortMode.ALPHABETICAL) ? SortMode.POINTS : SortMode.ALPHABETICAL;
                    this.rebuildWidgets();
                }
        ).bounds(5, 5, 150, 20).build();
        this.addRenderableWidget(sortButton);

        List<Item> items = getSortedItems(); // <- statt ClientKnownItemsCache.getItems() + manuellem sort

        int columns = 7;
        int cellSize = 64;
        int scrollbarPadding = 20;

        int gridWidth = (columns * cellSize) + scrollbarPadding;
        int startX = (this.width - gridWidth) / 2;

        int startY = 40;
        int gridHeight = this.height - 80;

        this.addRenderableWidget(new CollectedItemGridWidget(
                startX,
                startY,
                gridWidth,
                gridHeight,
                Component.literal("Items"),
                items
        ));
    }

    private String sortLabel() {
        return switch (sortMode) {
            case ALPHABETICAL -> "A-Z";
            case POINTS -> "Punkte";
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        double borderSize = ClientKnownItemsCache.getBorderSize();
        int discoveredItems = ClientKnownItemsCache.getItems().size();
        graphics.text(this.font, "Current Border Size: " + borderSize, 10, 35, 0xFFFFFFFF, true);
        graphics.text(this.font, "Item Count: " + discoveredItems, 10, 45 + this.font.lineHeight, 0xFFFFFFFF, true);
    }

    private enum SortMode {
        ALPHABETICAL,
        POINTS
    }

    private SortMode sortMode = SortMode.ALPHABETICAL;

    private List<Item> getSortedItems() {
        List<Item> items = new ArrayList<>(ClientKnownItemsCache.getItems());

        Comparator<Item> alphabetical = Comparator.comparing(
                item -> item.getDefaultInstance().getHoverName().getString()
        );

        if (sortMode == SortMode.ALPHABETICAL) {
            items.sort(alphabetical);
        } else {
            // Primär nach Punktzahl absteigend (höchste Punktzahl zuerst),
            // sekundär alphabetisch bei gleicher Punktzahl
            Comparator<Item> byPoints = Comparator.comparingInt(ItemPoints::getPoints).reversed();
            items.sort(byPoints.thenComparing(alphabetical));
        }

        return items;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
