package tech.komutech.objects.customs.parent;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractEmptyMachine<O extends MachineOperation> extends SlimefunItem implements InventoryBlock, MachineProcessHolder<O> {
   public AbstractEmptyMachine(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4) {
      super(var1, var2, var3, var4);
   }

   public abstract BlockTicker getBlockTicker();
}
