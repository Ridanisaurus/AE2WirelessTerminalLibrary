package de.mari_023.ae2wtlib;

import java.util.HashMap;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import de.mari_023.ae2wtlib.curio.CurioLocator;
import de.mari_023.ae2wtlib.networking.ServerNetworkManager;
import de.mari_023.ae2wtlib.networking.s2c.UpdateRestockPacket;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHost;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;

@Mod(AE2wtlib.MOD_NAME)
@Mod.EventBusSubscriber
public class AE2wtlibForge {
    public static final HashMap<String, Item> ITEMS = new HashMap<>();

    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS,
            AE2wtlib.MOD_NAME);

    public AE2wtlibForge() {
        AE2wtlibConfig.init();
        if (Platform.trinketsPresent())
            MenuLocators.register(CurioLocator.class, CurioLocator::writeToPacket, CurioLocator::readFromPacket);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        RECIPES.register(modEventBus);
        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registry.MENU_REGISTRY)) {
                AE2wtlib.registerMenus();
            } else if (event.getRegistryKey().equals(Registry.ITEM_REGISTRY)) {
                AE2wtlib.createItems();
                for (var entry : ITEMS.entrySet()) {
                    ForgeRegistries.ITEMS.register(entry.getKey(), entry.getValue());
                }
                AE2wtlib.onAe2Initialized();
            }
        });
    }

    @SubscribeEvent
    public static void handle(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        var item = event.getItem();

        if (restock(player, item, event.getResultStack().getCount()))
            return;

        event.setResultStack(item);
        ServerNetworkManager.sendToClient(player, new UpdateRestockPacket(
                player.getInventory().findSlotMatchingUnusedItem(item), item.getCount()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handle(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled())
            return;
        var item = event.getItemStack();

        if (restock(player, item, item.getCount()))
            return;

        player.setItemInHand(event.getHand(), item);
        ServerNetworkManager.sendToClient(player, new UpdateRestockPacket(
                player.getInventory().findSlotMatchingUnusedItem(item), item.getCount()));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void handle(EntityItemPickupEvent event) {
        if (event.isCanceled()) {
            return;
        }

        event.setCanceled(insertStackInME(event.getItem().getItem(), event.getEntity()));
    }

    private static boolean restock(ServerPlayer player, ItemStack item, int count) {
        if (player.isCreative() || item.isEmpty())
            return true;
        CraftingTerminalHandler cTHandler = CraftingTerminalHandler.getCraftingTerminalHandler(player);
        if (!cTHandler.inRange() || !ItemWT.getBoolean(cTHandler.getCraftingTerminal(), "restock")
                || cTHandler.getTargetGrid() == null || cTHandler.getTargetGrid().getStorageService() == null)
            return true;
        int toAdd = item.getMaxStackSize() / 2 - count;
        if (toAdd == 0)
            return true;

        long changed;
        if (toAdd > 0)
            changed = cTHandler.getTargetGrid().getStorageService().getInventory().extract(
                    AEItemKey.of(item), toAdd, Actionable.MODULATE,
                    new PlayerSource(player, cTHandler.getSecurityStation()));
        else
            changed = -cTHandler.getTargetGrid().getStorageService().getInventory().insert(
                    AEItemKey.of(item), -toAdd, Actionable.MODULATE,
                    new PlayerSource(player, cTHandler.getSecurityStation()));

        item.setCount(count + (int) changed);
        return false;
    }

    private static boolean insertStackInME(ItemStack stack, Player player) {
        if (stack.isEmpty())
            return false;
        CraftingTerminalHandler cTHandler = CraftingTerminalHandler.getCraftingTerminalHandler(player);
        ItemStack terminal = cTHandler.getCraftingTerminal();

        if (!(MagnetHandler.getMagnetSettings(terminal).magnetMode == MagnetMode.PICKUP_ME))
            return false;
        if (!cTHandler.inRange())
            return false;

        MagnetHost magnetHost = cTHandler.getMagnetHost();
        if (magnetHost == null)
            return false;
        if (!magnetHost.getInsertFilter().matchesFilter(AEItemKey.of(stack), magnetHost.getInsertMode()))
            return false;

        if (cTHandler.getTargetGrid() == null)
            return false;
        if (cTHandler.getTargetGrid().getStorageService() == null)
            return false;

        long inserted = cTHandler.getTargetGrid().getStorageService().getInventory().insert(AEItemKey.of(stack),
                stack.getCount(), Actionable.MODULATE, new PlayerSource(player, null));
        int leftover = (int) (stack.getCount() - inserted);
        if (leftover == 0) {
            stack.setCount(0);
            return true;
        } else
            stack.setCount(leftover);
        return false;
    }
}