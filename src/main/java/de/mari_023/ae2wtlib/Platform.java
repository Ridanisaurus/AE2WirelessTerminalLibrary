package de.mari_023.ae2wtlib;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fml.ModList;

import de.mari_023.ae2wtlib.curio.CurioHelper;
import de.mari_023.ae2wtlib.wut.recipe.Combine;
import de.mari_023.ae2wtlib.wut.recipe.CombineSerializer;
import de.mari_023.ae2wtlib.wut.recipe.Upgrade;
import de.mari_023.ae2wtlib.wut.recipe.UpgradeSerializer;

import appeng.menu.locator.MenuLocator;

public class Platform {

    public static boolean trinketsPresent() {
        return ModList.get().isLoaded("curios");
    }

    public static boolean isStillPresentTrinkets(Player player, ItemStack terminal) {
        return CurioHelper.isStillPresent(player, terminal);
    }

    public static ItemStack getItemStackFromTrinketsLocator(Player player, MenuLocator locator) {
        return CurioHelper.getItemStack(player, locator);
    }

    @Nullable
    public static MenuLocator findTerminalFromAccessory(Player player, String terminalName) {
        if (trinketsPresent()) {
            return CurioHelper.findTerminal(player, terminalName);
        }
        return null;
    }

    public static CreativeModeTab getCreativeModeTab() {
        return new CreativeModeTab(AE2wtlib.MOD_NAME) {
            @Override
            public ItemStack makeIcon() {
                return new ItemStack(AE2wtlib.UNIVERSAL_TERMINAL);
            }
        };
    }

    public static void registerItem(String name, Item item) {
        AE2wtlibForge.ITEMS.put(name, item);
    }

    public static void registerRecipe(String name, RecipeSerializer<?> serializer) {
        AE2wtlibForge.RECIPES.register(name, () -> serializer);
    }

    public static boolean preventRemoteMovement(ItemEntity item) {
        return item.getPersistentData().contains("PreventRemoteMovement");
    }
}
