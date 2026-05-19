package cz.autopotion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AutoPotionScreen extends Screen {

    private static final int PANEL_W = 340;
    private static final int ROW_H = 28;
    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 40;

    private int panelX, panelY, panelH;
    private final List<PotionRow> rows = new ArrayList<>();

    public AutoPotionScreen() {
        super(Text.literal("§6⚗ AutoPotion §7– Nastavení"));
    }

    @Override
    protected void init() {
        rows.clear();

        panelH = HEADER_H + AutoPotionConfig.rules.size() * ROW_H + FOOTER_H + 10;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;

        // Tlačítko Zapnout/Vypnout celý mod
        addDrawableChild(ButtonWidget.builder(
            Text.literal(AutoPotionConfig.enabled ? "§aMod: ZAPNUTO" : "§cMod: VYPNUTO"),
            btn -> {
                AutoPotionConfig.enabled = !AutoPotionConfig.enabled;
                btn.setMessage(Text.literal(AutoPotionConfig.enabled ? "§aMod: ZAPNUTO" : "§cMod: VYPNUTO"));
                AutoPotionConfig.save();
            }
        ).dimensions(panelX + 10, panelY + 18, 140, 20).build());

        // Slider pro delay
        addDrawableChild(new SliderWidget(
            panelX + 160, panelY + 18, 170, 20,
            Text.literal("Interval: " + (AutoPotionConfig.tickDelay / 20.0f) + "s"),
            (AutoPotionConfig.tickDelay - 10) / 90.0
        ) {
            @Override
            protected void updateMessage() {
                int ticks = 10 + (int) (value * 90);
                setMessage(Text.literal(String.format("Interval: %.1fs", ticks / 20.0f)));
            }

            @Override
            protected void applyValue() {
                AutoPotionConfig.tickDelay = 10 + (int) (value * 90);
                AutoPotionConfig.save();
            }
        });

        // Řádky pro každý potion
        for (int i = 0; i < AutoPotionConfig.rules.size(); i++) {
            AutoPotionConfig.PotionRule rule = AutoPotionConfig.rules.get(i);
            int rowY = panelY + HEADER_H + i * ROW_H;
            PotionRow row = new PotionRow(rule, rowY);
            rows.add(row);

            // Toggle tlačítko
            addDrawableChild(ButtonWidget.builder(
                Text.literal(rule.enabled ? "§a✔" : "§c✘"),
                btn -> {
                    rule.enabled = !rule.enabled;
                    btn.setMessage(Text.literal(rule.enabled ? "§a✔" : "§c✘"));
                    AutoPotionConfig.save();
                }
            ).dimensions(panelX + 8, rowY + 4, 22, 20).build());

            // HP threshold slider
            final int idx = i;
            addDrawableChild(new SliderWidget(
                panelX + 200, rowY + 4, 130, 20,
                Text.literal("HP: " + (int) rule.threshold + "%"),
                rule.threshold / 100.0
            ) {
                @Override
                protected void updateMessage() {
                    setMessage(Text.literal("HP: " + (int) (value * 100) + "%"));
                }

                @Override
                protected void applyValue() {
                    AutoPotionConfig.rules.get(idx).threshold = (float) (value * 100);
                    AutoPotionConfig.save();
                }
            });
        }

        // Tlačítko Zavřít
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§7Zavřít"),
            btn -> close()
        ).dimensions(panelX + PANEL_W / 2 - 50, panelY + panelH - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Tmavé pozadí
        renderInGameBackground(context);

        // Panel
        context.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, 0xCC1a1a2e);
        context.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, 0xFFf0c040); // zlatý proužek nahoře
        context.drawBorder(panelX, panelY, PANEL_W, panelH, 0xFF555555);

        // Nadpis
        context.drawCenteredTextWithShadow(textRenderer, "⚗ AutoPotion – Nastavení",
            panelX + PANEL_W / 2, panelY + 5, 0xFFf0c040);

        // Oddělovač pod headerem
        context.fill(panelX + 4, panelY + HEADER_H - 2, panelX + PANEL_W - 4, panelY + HEADER_H - 1, 0xFF444444);

        // Řádky potionů
        for (int i = 0; i < AutoPotionConfig.rules.size(); i++) {
            AutoPotionConfig.PotionRule rule = AutoPotionConfig.rules.get(i);
            int rowY = panelY + HEADER_H + i * ROW_H;

            // Střídavé pozadí
            if (i % 2 == 0) {
                context.fill(panelX + 2, rowY + 2, panelX + PANEL_W - 2, rowY + ROW_H, 0x22ffffff);
            }

            // Název potion
            String color = rule.enabled ? "§f" : "§7";
            context.drawTextWithShadow(textRenderer,
                Text.literal(color + rule.displayName),
                panelX + 36, rowY + 10, 0xFFFFFFFF);
        }

        // Legenda
        context.drawCenteredTextWithShadow(textRenderer,
            "§7✔/✘ = zapnout   |   slider = procento HP kdy hodit",
            panelX + PANEL_W / 2, panelY + panelH - 42, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false; // GUI nezastaví hru
    }

    private static class PotionRow {
        AutoPotionConfig.PotionRule rule;
        int y;
        PotionRow(AutoPotionConfig.PotionRule rule, int y) {
            this.rule = rule;
            this.y = y;
        }
    }
}
