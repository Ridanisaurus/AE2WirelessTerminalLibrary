package de.mari_023.ae2wtlib.datagen;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.util.AEColor;
import appeng.core.definitions.AEItems;
import appeng.datagen.providers.tags.ConventionTags;
import appeng.items.tools.powered.WirelessTerminalItem;

import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.recipe.Color;

public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        for (var color : AEColor.values()) {
            color(pRecipeOutput, AEItems.WIRELESS_CRAFTING_TERMINAL.asItem(), color);
            color(pRecipeOutput, AE2wtlibItems.PATTERN_ACCESS_TERMINAL, color);
            color(pRecipeOutput, AE2wtlibItems.PATTERN_ENCODING_TERMINAL, color);
            color(pRecipeOutput, AE2wtlibItems.UNIVERSAL_TERMINAL, color);
            color(pRecipeOutput, AEItems.WIRELESS_TERMINAL.asItem(), color);
        }
    }

    private static void color(RecipeOutput consumer, WirelessTerminalItem terminal, AEColor color) {
        TagKey<Item> dye;
        if (color == AEColor.TRANSPARENT) {
            dye = ConventionTags.CAN_REMOVE_COLOR;
        } else
            dye = color.dye.getTag();
        consumer.accept(
                AE2wtlib.id("color_" + Objects.requireNonNull(terminal.getRegistryName()).getPath() + "_"
                        + color.registryPrefix),
                new Color(Ingredient.of(terminal), Ingredient.of(dye), color,
                        new ItemStack(terminal)),
                null);
    }
}