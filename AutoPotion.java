package cz.autopotion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.potion.Potion;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AutoPotion implements ModInitializer {
    public static final String MOD_ID = "autopotion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openGuiKey;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        AutoPotionConfig.load();

        // GLFW_KEY_APOSTROPHE = fyzická poloha klávesy é na české klávesnici
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autopotion.opengui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE,
            "category.autopotion"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Otevření GUI klávesou P
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new AutoPotionScreen());
                }
            }

            // Logika házení potionů
            if (client.player == null || client.world == null) return;
            if (!AutoPotionConfig.enabled) return;

            tickCounter++;
            if (tickCounter < AutoPotionConfig.tickDelay) return;
            tickCounter = 0;

            float health = client.player.getHealth();
            float maxHealth = client.player.getMaxHealth();
            float healthPercent = (health / maxHealth) * 100f;

            // Zkontroluj každý povolený typ potion
            for (AutoPotionConfig.PotionRule rule : AutoPotionConfig.rules) {
                if (!rule.enabled) continue;
                if (healthPercent > rule.threshold) continue;

                // Najdi potion v inventáři
                int slot = findPotionInInventory(client, rule.potionType);
                if (slot == -1) continue;

                // Přesuň do ruky a hoď
                throwPotion(client, slot);
                break; // hoď jen jeden per tick cycle
            }
        });

        LOGGER.info("AutoPotion mod načten! Stiskni P pro nastavení.");
    }

    private int findPotionInInventory(MinecraftClient client, String potionType) {
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            // Hledáme throwable potions (Items.SPLASH_POTION nebo LINGERING_POTION)
            if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents == null) continue;

                Optional<RegistryEntry<Potion>> potion = contents.potion();
                if (potion.isEmpty()) continue;

                String key = potion.get().getKey().map(k -> k.getValue().getPath()).orElse("");
                if (key.contains(potionType)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void throwPotion(MinecraftClient client, int slot) {
        PlayerInventory inv = client.player.getInventory();
        int previousSlot = inv.selectedSlot;

        // Pokud je potion v hlavním baru (0-8), vyber ho
        if (slot < 9) {
            inv.selectedSlot = slot;
            client.getNetworkHandler().sendPacket(
                new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, client.player.getYaw(), client.player.getPitch())
            );
            inv.selectedSlot = previousSlot;
        } else {
            // Potion je v ostatním inventáři — přesuň ho do ruky (slot 0)
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slot, 0,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                client.player
            );
            inv.selectedSlot = 0;
            client.getNetworkHandler().sendPacket(
                new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, client.player.getYaw(), client.player.getPitch())
            );
            // Vrať zpět
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slot, 0,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                client.player
            );
            inv.selectedSlot = previousSlot;
        }
    }
}
