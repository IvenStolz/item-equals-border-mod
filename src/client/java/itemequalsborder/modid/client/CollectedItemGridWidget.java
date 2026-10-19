package itemequalsborder.modid.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scrollable grid container that arranges and renders {@link CollectedItemWidget} instances.
 */
public class CollectedItemGridWidget extends AbstractScrollArea {
    private final List<CollectedItemWidget> itemWidgets = new ArrayList<>();
    private final int cellSize = 64;

    /**
     * Calculates the number of grid columns based on the available widget width.
     */
    private int getColumns() {
        return Math.max(1, this.width / cellSize);
    }

    /**
     * Constructs the scrollable item grid widget.
     *
     * @param x               The X position of the grid.
     * @param y               The Y position of the grid.
     * @param width           The width of the grid.
     * @param height          The height of the grid.
     * @param title           The title component of the scroll area.
     * @param allItems        The list of items to display.
     * @param discoveredItems The set of already discovered items.
     */
    public CollectedItemGridWidget(int x, int y, int width, int height, Component title,
                                   List<Item> allItems, Set<Item> discoveredItems) {
        super(x, y, width, height, title, new AbstractScrollArea.ScrollbarSettings(
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller"),
                null,
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller_background"),
                6, 8, 10, true
        ));
        setItems(allItems, discoveredItems);
    }

    /**
     * Replaces the current grid items and rebuilds the child item widgets.
     *
     * @param allItems        The updated list of items to display.
     * @param discoveredItems The updated set of discovered items.
     */
    public void setItems(List<Item> allItems, Set<Item> discoveredItems) {
        this.itemWidgets.clear();
        for (Item item : allItems) {
            boolean discovered = discoveredItems.contains(item);
            this.itemWidgets.add(new CollectedItemWidget(0, 0, 32, new ItemStack(item), discovered));
        }
    }

    /**
     * Returns the total vertical height of the grid content for scrollbar calculation.
     */
    @Override
    protected int contentHeight() {
        int rows = (int) Math.ceil((double) itemWidgets.size() / getColumns());
        return rows * cellSize;
    }

    /**
     * Returns the X coordinate of the scrollbar.
     */
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }

    /**
     * Positions and renders all visible item widgets within the current scroll viewport.
     */
    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int col = 0;
        int row = 0;

        for (CollectedItemWidget widget : itemWidgets) {
            // Calculate absolute position of the current grid cell
            int cellX = this.getX() + col * cellSize;
            int cellY = this.getY() + row * cellSize - (int) this.scrollAmount();

            // Center the 32x32 widget inside the 64x64 cell with a top padding
            int widgetX = cellX + (cellSize - widget.getWidth()) / 2;
            int widgetY = cellY + 5;

            // Update widget coordinates (required for hover detection)
            widget.setX(widgetX);
            widget.setY(widgetY);

            // Only render the widget if it is within the visible scroll area
            if (widgetY + widget.getHeight() > this.getY() && widgetY < this.getY() + this.height) {
                widget.extractRenderState(graphics, mouseX, mouseY, a);
            }

            col++;
            if (col >= getColumns()) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
    }
}