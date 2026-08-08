package itemequalsborder.modid.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CollectedItemGridWidget extends AbstractScrollArea {
    private final List<CollectedItemWidget> itemWidgets = new ArrayList<>();
    private final int columns = 7;
    private final int cellSize = 64;

    public CollectedItemGridWidget(int x, int y, int width, int height, Component title, List<Item> items) {
        super(x, y, width, height, title, new AbstractScrollArea.ScrollbarSettings(
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller"),
                null,
                Identifier.fromNamespaceAndPath("minecraft", "widget/scroller_background"),
                6,
                8,
                10,
                true
        ));
        for (Item item : items) {
            this.itemWidgets.add(new CollectedItemWidget(0, 0, 32, new ItemStack(item)));
        }
    }

    // 1. Teile dem Widget mit, wie hoch der INHALT insgesamt ist
    @Override
    protected int contentHeight() {
        int rows = (int) Math.ceil((double) itemWidgets.size() / columns);
        return rows * cellSize;
    }

    // Optional: Position des Scrollbalkens anpassen
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int col = 0;
        int row = 0;

        for (CollectedItemWidget widget : itemWidgets) {
            // 2. Berechne die absolute Position der aktuellen Raster-Zelle
            int cellX = this.getX() + col * cellSize;
            int cellY = this.getY() + row * cellSize - (int) this.scrollAmount();

            // 3. Zentriere dein 32x32 Widget innerhalb der 64x64 Zelle
            int widgetX = cellX + (cellSize - widget.getWidth()) / 2;
            int widgetY = cellY + 5; // Leichter Puffer nach oben

            // 4. Update die Widget-Koordinaten (wichtig für die isHovered()-Erkennung!)
            widget.setX(widgetX);
            widget.setY(widgetY);

            // 5. Zeichne das Widget nur, wenn es sich im sichtbaren Bereich befindet (Optimierung)
            if (widgetY + widget.getHeight() > this.getY() && widgetY < this.getY() + this.height) {
                widget.extractRenderState(graphics, mouseX, mouseY, a);
            }

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}