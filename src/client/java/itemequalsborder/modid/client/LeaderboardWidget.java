package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class LeaderboardWidget extends AbstractWidget {

    private static final int PADDING = 10;

    public LeaderboardWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Rangliste"));
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var font = net.minecraft.client.Minecraft.getInstance().font;

        // Trennlinie am linken Rand des Widgets
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), 0xFF555555);

        int textX = this.getX() + PADDING;
        int y = this.getY();

        graphics.text(font, "Rangliste", textX, y, 0xFFFFFF55, true);
        y += font.lineHeight + 8;

        List<Item> items = ClientKnownItemsCache.getItems();
        List<String> discoverers = ClientKnownItemsCache.getDiscoverersList();

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            int points = ItemPoints.getPoints(items.get(i));
            scores.merge(discoverers.get(i), points, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int rank = 1;
        int bottomLimit = this.getY() + this.getHeight();

        for (Map.Entry<String, Integer> entry : sorted) {
            if (y + font.lineHeight * 2 > bottomLimit) break; // nicht über die Widget-Höhe hinaus zeichnen

            String line = rank + ". " + entry.getKey();
            String scoreLine = entry.getValue() + " Punkte";

            graphics.text(font, line, textX, y, 0xFFFFFFFF, true);
            y += font.lineHeight;
            graphics.text(font, scoreLine, textX + 8, y, 0xFFAAAAAA, true);
            y += font.lineHeight + 6;

            rank++;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // Für Screenreader vorerst übersprungen
    }
}