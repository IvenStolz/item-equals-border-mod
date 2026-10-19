package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import itemequalsborder.modid.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Sidebar widget that displays a ranked leaderboard of players based on their collected item points.
 */
public class LeaderboardWidget extends AbstractWidget {

    private static final int PADDING = 10;
    private static final int HEAD_SIZE = 16;
    private static final int LINE_GAP = 6;

    /**
     * Constructs the leaderboard widget.
     *
     * @param x      The X position of the widget.
     * @param y      The Y position of the widget.
     * @param width  The width of the widget.
     * @param height The height of the widget.
     */
    public LeaderboardWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Lang.tr("leaderboard.title"));
    }

    /**
     * Renders the leaderboard header, player skins, names, and point totals.
     */
    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;

        // Vertical separator line on the left border of the widget
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), 0xFF555555);

        int textX = this.getX() + PADDING;
        int y = this.getY();

        graphics.text(font, this.getMessage(), textX, y, 0xFFFFFF55, true);
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

        int headX = textX;
        int nameX = headX + HEAD_SIZE + 6;
        int rowHeight = Math.max(HEAD_SIZE, font.lineHeight * 2) + LINE_GAP;
        int bottomLimit = this.getY() + this.getHeight();

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (y + rowHeight > bottomLimit) break;

            String playerName = entry.getKey();
            int score = entry.getValue();
            Identifier skin = resolveSkin(playerName);

            // Render player head (base layer and hat overlay)
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, headX, y, 16f, 16f, 16, 16, 128, 128, 0xFFFFFFFF);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, headX, y, 80f, 16f, 16, 16, 128, 128, 0xFFFFFFFF);

            Component pointsText = Lang.tr(score == 1 ? "points.one" : "points.other", score);

            graphics.text(font, rank + ". " + playerName, nameX, y, 0xFFFFFFFF, true);
            graphics.text(font, pointsText, nameX, y + font.lineHeight, 0xFFAAAAAA, true);

            y += rowHeight;
            rank++;
        }
    }

    /**
     * Resolves the skin texture identifier for a given player name.
     *
     * @param playerName The name of the player.
     * @return The player's skin texture, or the default skin if offline/unavailable.
     */
    private Identifier resolveSkin(String playerName) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getListedOnlinePlayers()) {
                if (info.getProfile().name().equalsIgnoreCase(playerName)) {
                    return info.getSkin().body().texturePath();
                }
            }
        }
        // Fallback to default texture if the player is offline
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // Skipped for screen readers for now
    }
}