package tech.komutech.objects.customs;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuCloseHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuOpeningHandler;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.script.ScriptEval;
import tech.komutech.util.colors.CMIChatColor;
import tech.komutech.objects.customs.machine.CustomMachine;
import tech.komutech.objects.customs.machine.CustomNoEnergyMachine;
import tech.komutech.objects.customs.machine.CustomRecipeMachine;
import tech.komutech.objects.script.lambda.RSCClickHandler;

public class CustomMenu {
   private final ScriptEval eval;
   private final String title;
   private final String id;
   private int progressSlot;
   private int size;
   private ItemStack progress;
   private boolean playerInvClickable;
   private MenuOpeningHandler menuOpeningHandler;
   private MenuCloseHandler menuCloseHandler;
   private final Map<Integer, ItemStack> items;
   private final Map<Integer, MenuClickHandler> clickHandlers;
   private final Set<Integer> interactableSlots = new HashSet<>();

   public CustomMenu(String var1, String var2, CustomMenu var3) {
      this.menuOpeningHandler = var0 -> {};
      this.menuCloseHandler = var0 -> {};
      this.eval = var3.eval;
      this.title = CMIChatColor.translate(var2);
      this.id = var1;
      this.items = var3.items;
      this.progressSlot = var3.progressSlot;
      this.clickHandlers = var3.clickHandlers;
      this.interactableSlots.addAll(var3.interactableSlots);
      this.size = var3.size;
      if (this.eval != null) {
         this.menuOpeningHandler = var3.menuOpeningHandler;
         this.menuCloseHandler = var3.menuCloseHandler;
      }
   }

   public CustomMenu(String var1, String var2, @Nullable BlockMenuPreset var3, @Nullable ItemStack var4, @Nullable ScriptEval var5) {
      this(var1, var2, new HashMap<>(), var3 == null || var3.isPlayerInventoryClickable(), 22, var4, var5);
      if (var3 != null) {
         this.cloneFromPresetInventory(var3);
         SlimefunItem var6 = (SlimefunItem)Slimefun.getRegistry().getSlimefunItemIds().get(var3.getID());
         if (var6 instanceof CustomMachine var7) {
            this.progressSlot = var7.getMenu().getProgressSlot();
            this.progress = var7.getMenu().getProgressBarItem();
         } else if (var6 instanceof CustomNoEnergyMachine var8) {
            this.progressSlot = var8.getMenu().getProgressSlot();
            this.progress = var8.getMenu().getProgressBarItem();
         } else if (var6 instanceof CustomRecipeMachine var9) {
            this.progressSlot = var9.getMenu() != null ? var9.getMenu().getProgressSlot() : 22;
            this.progress = var9.getProgressBar();
         } else if (var6 instanceof AContainer var10) {
            this.progressSlot = 22;
            this.progress = var10.getProgressBar();
         }

         this.size = var3.getSize();
      }
   }

   public CustomMenu(
      String var1, String var2, @NotNull Map<Integer, ItemStack> var3, boolean var4, int var5, @Nullable ItemStack var6, @Nullable ScriptEval var7
   ) {
      this.menuOpeningHandler = var0 -> {};
      this.menuCloseHandler = var0 -> {};
      this.id = var1;
      this.title = CMIChatColor.translate(var2);
      this.eval = var7;
      this.progress = var6 != null ? var6.clone() : (ItemStack)var3.get(var5);
      this.progressSlot = var5;
      this.playerInvClickable = var4;
      this.items = var3;
      this.clickHandlers = new HashMap<>();
      if (var7 != null) {
         var7.doInit();
      }

      for (int var8 = 0; var8 < 54; var8++) {
         ItemStack var9 = this.items.get(var8);
         if (var9 != null) {
            this.addItem(var8, var9, (var1x, var2x, var3x, var4x) -> this.resolveDecoratedSlotClick(var1x, var2x, var3x, var4x));
         }
      }

      if (var7 != null) {
         this.menuOpeningHandler = var1x -> var7.evalFunction("onOpen", new Object[]{var1x});
         this.menuCloseHandler = var1x -> var7.evalFunction("onClose", new Object[]{var1x});
      }
   }

   public String getId() {
      return this.id;
   }

   public CustomMenu setSize(int var1) {
      if (var1 == -1) {
         this.size = -1;
         return this;
      } else if (var1 > 54 || var1 < 0) {
         throw new IllegalArgumentException("Size must be between 0 and 54");
      } else if (var1 % 9 != 0) {
         throw new IllegalArgumentException("Size must be a multiple of 9");
      } else {
         this.size = var1;
         return this;
      }
   }

   public void addItem(int var1, ItemStack var2, MenuClickHandler var3) {
      if (!var2.getItemMeta().hasDisplayName()) {
         var2.editMeta(var0 -> var0.displayName(Component.text(" ")));
      }

      this.items.put(var1, var2);
      this.clickHandlers.put(var1, var3);
   }

   public void addItem(int var1, ItemStack var2) {
      this.items.put(var1, var2);
   }

   public CustomMenu registerInteractableSlots(int... var1) {
      if (var1 != null) {
         for (int var5 : var1) {
            this.interactableSlots.add(var5);
         }
      }

      return this;
   }

   public CustomMenu registerInteractableSlots(Iterable<Integer> var1) {
      if (var1 != null) {
         for (Integer var3 : var1) {
            if (var3 != null) {
               this.interactableSlots.add(var3);
            }
         }
      }

      return this;
   }

   /**
    * 装饰槽（按钮 / 边框 / 进度条等）的点击兜底判定。
    * <p>
    * 返回值语义见 Slimefun 的 {@code MenuListener#handleEvent}：返回 {@code false} 会
    * {@code event.setCancelled(true)}（取消点击），返回 {@code true} 则放行。
    * <p>
    * 修复：原先当槽位上仍是装饰占位物品时，会返回 {@code hasItemOnCursor(player)}——
    * 玩家"鼠标上提着物品"点击按钮时返回 {@code true} 放行，于是 Minecraft 会用光标物品
    * 与槽位交换，把机器 UI 的按钮扣下来。装饰按钮任何情况下都不允许被拿走或替换。
    */
   private boolean resolveDecoratedSlotClick(Player var1, int var2, ItemStack var3, ClickAction var4) {
      if (this.eval != null && this.eval.evalFunction("onClick", new Object[]{var1, var2, var3, var4}) instanceof Boolean var6) {
         return var6;
      } else if (var2 == this.progressSlot) {
         return false;
      } else {
         ItemStack var7 = this.items.get(var2);
         if (var7 == null) {
            return true;
         } else if (var3 == null || var3.getType().isAir()) {
            return true;
         } else {
            // 槽位里已换成真实产物（非装饰占位）→ 放行，允许取出；
            // 仍是装饰占位按钮 → 一律取消，禁止拿取 / 与光标物品交换。
            return !matchesPlaceholder(var3, var7);
         }
      }
   }

   private static boolean matchesPlaceholder(ItemStack var0, ItemStack var1) {
      ItemStack var2 = var0.clone();
      ItemStack var3 = var1.clone();
      var2.setAmount(1);
      var3.setAmount(1);
      return var2.isSimilar(var3);
   }

   private MenuClickHandler getClickHandler(int var1) {
      return this.clickHandlers.getOrDefault(var1, (var0, var1x, var2, var3) -> true);
   }

   @Nullable
   public ItemStack getProgressBarItem() {
      return this.progress;
   }

   public String getID() {
      return this.id;
   }

   private void cloneFromPresetInventory(BlockMenuPreset var1) {
      var1.getContents();
      Inventory var2 = Bukkit.createInventory(null, var1.toInventory().getSize(), CMIChatColor.translate(this.title));

      for (int var3 = 0; var3 < var1.getInventory().getSize(); var3++) {
         ItemStack var5 = var1.getItemInSlot(var3);
         if (var5 != null) {
            this.addItem(var3, var5.clone());
            var2.setItem(var3, var5.clone());
         }

         final MenuClickHandler var4;
         if ((var4 = var1.getMenuClickHandler(var3)) != null) {
            this.addMenuClickHandler(var3, new RSCClickHandler() {
               @Override
               public void mainFunction(Player var1, int var2x, ItemStack var3x, ClickAction var4x) {
                  var4.onClick(var1, var2x, var3x, var4x);
               }

               @Override
               public void andThen(Player var1, int var2x, ItemStack var3x, ClickAction var4x) {
                  if (CustomMenu.this.eval != null) {
                     CustomMenu.this.eval.evalFunction("onClick", new Object[]{var1, var2x, var3x, var4x});
                  }
               }
            });
         }
      }
   }

   public void addMenuClickHandler(int var1, MenuClickHandler var2) {
      this.clickHandlers.put(var1, var2);
   }

   public void apply(BlockMenuPreset var1) {
      var1.setPlayerInventoryClickable(this.playerInvClickable);
      if (this.size != -1) {
         var1.setSize(this.size);
      }

      for (int var3 : this.items.keySet()) {
         if (!this.interactableSlots.contains(var3)) {
            var1.addItem(var3, this.items.get(var3), this.getClickHandler(var3));
         }
      }

      for (int var5 : this.interactableSlots) {
         var1.addMenuClickHandler(
            var5,
            (var1x, var2, var3x, var4) -> this.eval != null
                  && this.eval.evalFunction("onClick", new Object[]{var1x, var2, var3x, var4}) instanceof Boolean var6
               ? var6
               : true
         );
      }

      var1.addMenuOpeningHandler(this.menuOpeningHandler);
      var1.addMenuCloseHandler(this.menuCloseHandler);
   }

   @Nullable
   public MenuClickHandler getMenuClickHandler(int var1) {
      return this.clickHandlers.get(var1);
   }

   public ScriptEval getEval() {
      return this.eval;
   }

   public String getTitle() {
      return this.title;
   }

   public int getProgressSlot() {
      return this.progressSlot;
   }

   public int getSize() {
      return this.size;
   }

   public ItemStack getProgress() {
      return this.progress;
   }

   public void setPlayerInvClickable(boolean var1) {
      this.playerInvClickable = var1;
   }

   public void setMenuOpeningHandler(MenuOpeningHandler var1) {
      this.menuOpeningHandler = var1;
   }

   public void setMenuCloseHandler(MenuCloseHandler var1) {
      this.menuCloseHandler = var1;
   }

   public Map<Integer, ItemStack> getItems() {
      return this.items;
   }
}
