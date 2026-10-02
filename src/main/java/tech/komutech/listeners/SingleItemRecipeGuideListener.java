package tech.komutech.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.KT;
import tech.komutech.native_scripts.support.JegBridge;
import tech.komutech.util.colors.CMIChatColor;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.LinkedOutput;
import tech.komutech.objects.customs.machine.CustomLinkedRecipeMachine;
import tech.komutech.objects.customs.machine.CustomRecipeMachine;
import tech.komutech.objects.customs.machine.CustomTemplateMachine;
import tech.komutech.objects.customs.machine.CustomWorkbench;
import tech.komutech.objects.machine.CustomLinkedMachineRecipe;
import tech.komutech.objects.machine.CustomMachineRecipe;
import tech.komutech.objects.machine.MachineTemplate;
import tech.komutech.objects.slimefun.AsyncChanceRecipeTask;
import tech.komutech.util.CommonUtils;

public class SingleItemRecipeGuideListener implements Listener {
   private static final NamespacedKey RECIPE_KEY = new NamespacedKey(KT.plugin(), "rsc_recipe");
   private static final NamespacedKey RECIPE_INDEX_KEY = new NamespacedKey(KT.plugin(), "rsc_recipe_index");
   private static final NamespacedKey RECIPE_TEMPLATE_INDEX_KEY = new NamespacedKey(KT.plugin(), "rsc_recipe_template_index");

   public SingleItemRecipeGuideListener() {
      Bukkit.getPluginManager().registerEvents(this, KT.plugin());
   }

   @EventHandler
   public void onClick(InventoryClickEvent var1) {
      Player var7 = (Player)var1.getWhoClicked();
      Inventory var8 = var1.getInventory();
      ItemStack var9 = var1.getCurrentItem();
      ChestMenu var2;
      SlimefunItem var3;
      ItemStack var4;
      Integer var6;
      if (isTaggedItem(var9)
         && (var6 = (Integer)var9.getItemMeta().getPersistentDataContainer().get(RECIPE_INDEX_KEY, PersistentDataType.INTEGER)) != null
         && var6 >= 0
         && (var4 = var8.getItem(16)) != null
         && (var3 = SlimefunItem.getByItem(var4)) != null
         && (var2 = this.createGUI(var7, var3, var9.getItemMeta())) != null) {
         var2.open(new Player[]{var7});
      }
   }

   private static boolean isTaggedItem(ItemStack var0) {
      if (var0 != null && var0.getItemMeta() != null) {
         PersistentDataContainer var1 = var0.getItemMeta().getPersistentDataContainer();
         return var1.getKeys().contains(RECIPE_KEY) && var1.getKeys().contains(RECIPE_INDEX_KEY);
      } else {
         return false;
      }
   }

   public static ItemStack tagItemRecipe(ItemStack var0, int var1) {
      var0 = var0.clone();
      ItemMeta var2 = var0.getItemMeta();
      PersistentDataContainer var3 = var2.getPersistentDataContainer();
      var3.set(RECIPE_KEY, PersistentDataType.INTEGER, 1);
      var3.set(RECIPE_INDEX_KEY, PersistentDataType.INTEGER, var1);
      var0.setItemMeta(var2);
      return var0;
   }

   public static ItemStack tagItemTemplateRecipe(ItemStack var0, int var1, int var2) {
      var0 = var0.clone();
      ItemMeta var3 = var0.getItemMeta();
      PersistentDataContainer var4 = var3.getPersistentDataContainer();
      var4.set(RECIPE_KEY, PersistentDataType.INTEGER, 2);
      var4.set(RECIPE_INDEX_KEY, PersistentDataType.INTEGER, var2);
      var4.set(RECIPE_TEMPLATE_INDEX_KEY, PersistentDataType.INTEGER, var1);
      var0.setItemMeta(var3);
      return var0;
   }

   /**
    * 材料/产物槽位的统一点击处理：打开该物品的配方展示页。
    *
    * <p><b>JEG 兼容</b>：原实现直接 {@code new SurvivalSlimefunGuide(...)}，
    * 而 {@code SurvivalSlimefunGuide} 是Slimefun 的硬编码原生实现——字节码里
    * 没有任何「查当前生效指南实现」的分支，所以装了 JEG 也只会弹出 Slimefun 原生界面。
    * 现改为先经 {@link JegBridge} 取玩家当前生效的指南实现（装了 JEG 就是 JEG），
    * 拿不到才回退原生。没装 JEG 的服务器行为与改动前完全一致。
    */
   public static boolean viewItem(Player var0, int var1, ItemStack var2, ClickAction var3) {
      if (JegBridge.displayItem(var0, var2)) {
         return false;
      }

      // 回退原生：与改动前一致
      PlayerProfile.find(var0).ifPresent(var4 -> new SurvivalSlimefunGuide(false, false).displayItem(var4, var2, 0, true));
      return false;
   }

   public static ItemStack tagItemLinkedRecipe(ItemStack var0, int var1) {
      var0 = var0.clone();
      ItemMeta var2 = var0.getItemMeta();
      PersistentDataContainer var3 = var2.getPersistentDataContainer();
      var3.set(RECIPE_KEY, PersistentDataType.INTEGER, 3);
      var3.set(RECIPE_INDEX_KEY, PersistentDataType.INTEGER, var1);
      var0.setItemMeta(var2);
      return var0;
   }

   public static ItemStack tagItemWorkbenchRecipe(ItemStack var0, int var1) {
      var0 = var0.clone();
      ItemMeta var2 = var0.getItemMeta();
      PersistentDataContainer var3 = var2.getPersistentDataContainer();
      var3.set(RECIPE_KEY, PersistentDataType.INTEGER, 4);
      var3.set(RECIPE_INDEX_KEY, PersistentDataType.INTEGER, var1);
      var0.setItemMeta(var2);
      return var0;
   }

   private ChestMenu createGUI(Player var1, SlimefunItem var2, PersistentDataHolder var3) {
      int var4 = PersistentDataAPI.getInt(var3, RECIPE_KEY, 1);
      if (var2 instanceof AContainer var5 && var4 == 1) {
         int var13 = PersistentDataAPI.getInt(var3, RECIPE_INDEX_KEY, 0);
         return new SingleItemRecipeGuideListener.RecipeMenu(var5, var1, var13);
      } else if (var2 instanceof CustomTemplateMachine var8 && var4 == 2) {
         int var12 = PersistentDataAPI.getInt(var3, RECIPE_TEMPLATE_INDEX_KEY, 0);
         int var7 = PersistentDataAPI.getInt(var3, RECIPE_INDEX_KEY, 0);
         return new SingleItemRecipeGuideListener.TemplateRecipeMenu(var8, var1, var12, var7);
      } else if (var2 instanceof CustomLinkedRecipeMachine var9 && var4 == 3) {
         int var11 = PersistentDataAPI.getInt(var3, RECIPE_INDEX_KEY, 0);
         return new SingleItemRecipeGuideListener.LinkedRecipeMenu(var9, var1, var11);
      } else if (var2 instanceof CustomWorkbench var10 && var4 == 4) {
         int var6 = PersistentDataAPI.getInt(var3, RECIPE_INDEX_KEY, 0);
         return new SingleItemRecipeGuideListener.WorkbenchRecipeMenu(var10, var1, var6);
      } else {
         return null;
      }
   }

   private static class LinkedRecipeMenu extends ChestMenu {
      private final AsyncChanceRecipeTask recipeTask = new AsyncChanceRecipeTask();

      public LinkedRecipeMenu(AContainer var1, Player var2, int var3) {
         super(Slimefun.getLocalization().getMessage(var2, "guide.title.main"));
         CustomMenu var4 = null;
         Optional<PlayerProfile> var5 = PlayerProfile.find(var2);
         this.setEmptySlotsClickable(false);
         this.setPlayerInventoryClickable(false);
         boolean var6 = true;
         int var7 = 31;
         ItemStack var8 = var1.getProgressBar();
         int[] var9 = var1.getInputSlots();
         int[] var10 = var1.getOutputSlots();
         if (var1 instanceof CustomLinkedRecipeMachine var11) {
            var4 = var11.getMenu();
            if (var4 != null) {
               var6 = false;
               var7 = var4.getProgressSlot();
               if (var4.getProgressBarItem() != null) {
                  var8 = var4.getProgressBarItem();
               }
            }
         } else {
            var9 = new int[]{28, 29};
            var10 = new int[]{33, 34};
         }

         BlockMenuPreset var24 = (BlockMenuPreset)Slimefun.getRegistry().getMenuPresets().get(var1.getId());
         if (var24 != null) {
            if (!var6) {
               for (Integer var13 : var24.getPresetSlots()) {
                  ItemStack var14 = var24.getItemInSlot(var13);
                  if (var14 != null) {
                     this.addItem(var13, var14, (var0, var1x, var2x, var3x) -> false);
                  }
               }
            }

            if (var6) {
               int[] var25 = new int[]{1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 22, 31, 40, 45, 46, 47, 48, 49, 50, 51, 52, 53};

               for (int var16 : var25) {
                  this.addItem(var16, ChestMenuUtils.getBackground(), (var0, var1x, var2x, var3x) -> false);
               }

               int[] var28 = new int[]{18, 19, 20, 21, 27, 30, 36, 37, 38, 39};

               for (int var17 : var28) {
                  this.addItem(var17, ChestMenuUtils.getInputSlotTexture(), (var0, var1x, var2x, var3x) -> false);
               }

               int[] var33 = new int[]{23, 24, 25, 26, 32, 35, 41, 42, 43, 44};

               for (int var18 : var33) {
                  this.addItem(var18, ChestMenuUtils.getOutputSlotTexture(), (var0, var1x, var2x, var3x) -> false);
               }

               var5.ifPresent(
                  var2x -> this.addItem(
                     0, ChestMenuUtils.getBackButton(var2, new String[]{"", "&f左键: &7返回上一页", "&fShift + 左键: &7返回主菜单"}), (var1xx, var2xx, var3x, var4x) -> {
                        // 返回按钮同样要走当前生效的指南实现（JEG），否则会被丢回原生界面
                        if (!JegBridge.back(var2x, var2x.getPlayer(), var4x.isShiftClicked())) {
                           SurvivalSlimefunGuide var5x = new SurvivalSlimefunGuide(false, false);
                           GuideHistory var6x = var2x.getGuideHistory();
                           if (var4x.isShiftClicked()) {
                              var5x.openMainMenu(var2x, var6x.getMainMenuPage());
                           } else {
                              var6x.goBack(var5x);
                           }
                        }

                        return false;
                     }
                  )
               );
            }

            MachineRecipe var26 = (MachineRecipe)var1.getMachineRecipes().get(var3);
            if (var26 instanceof CustomLinkedMachineRecipe var29) {
               Map<Integer, ItemStack> var34 = var29.getLinkedInput();

               for (int var45 : var34.keySet()) {
                  ItemStack var50 = (ItemStack)var34.get(var45);
                  if (var50 != null) {
                     this.addItem(var45, var50.clone(), SingleItemRecipeGuideListener::viewItem);
                  }
               }

               int var41 = var10[0];
               ItemStack[] var46 = var26.getOutput();
               if (var29.isChooseOneIfHas()) {
                  ArrayList var51 = new ArrayList();

                  for (int var53 = 0; var53 < var46.length; var53++) {
                     Integer var19 = var29.getChances().get(var53);
                     ItemStack var20 = var46[var53];
                     if (var19 != null && var19 > 0 && var20 != null) {
                        var51.add(this.tagOutputChance(var20, var19));
                     }
                  }

                  this.recipeTask.add(var41, var51);
                  this.addMenuClickHandler(var41, SingleItemRecipeGuideListener::viewItem);
               } else {
                  List var52 = var29.getChances();

                  for (int var54 = 0; var54 < var46.length; var54++) {
                     int var55 = (Integer)var52.get(var54);
                     ItemStack var56 = var46[var54];
                     if (var56 != null) {
                        ItemStack var21 = var56.clone();
                        if (var55 < 100) {
                           CommonUtils.addLore(var21, true, CMIChatColor.translate("&a有&b " + var55 + "% &a的概率产出"));
                        }

                        if (var55 > 0) {
                           this.addItem(var10[var54], var21, SingleItemRecipeGuideListener::viewItem);
                        }
                     }
                  }
               }
            } else {
               ItemStack[] var42 = var26.getInput();

               for (int var35 = 0; var35 < var42.length; var35++) {
                  ItemStack var47 = var42[var35];
                  if (var47 != null) {
                     this.addItem(var9[var35], var47, SingleItemRecipeGuideListener::viewItem);
                  }
               }

               for (int var36 = 0; var36 < var10.length; var36++) {
                  ItemStack var48 = var26.getOutput()[var36];
                  if (var48 != null) {
                     this.addItem(var10[var36], var48, SingleItemRecipeGuideListener::viewItem);
                  }
               }
            }

            int var30 = var26.getTicks() / 2;
            String var37 = "&e制作时间: &b" + var30 + "&es";
            if (var30 > 60) {
               var37 = var37.concat("(" + CommonUtils.formatSeconds(var30) + "&e)");
            }

            CustomItemStack var23 = new CustomItemStack(var8, var37, new String[0]);
            this.addItem(var7, var23, SingleItemRecipeGuideListener::viewItem);
         }
      }

      public void open(Player... var1) {
         super.open(var1);
         if (!this.recipeTask.isEmpty()) {
            this.recipeTask.start(this.toInventory());
         }
      }

      private ItemStack tagOutputChance(ItemStack var1, int var2) {
         var1 = var1.clone();
         CommonUtils.addLore(var1, true, CMIChatColor.translate("&a有&b " + var2 + "% &a的概率产出"));
         return var1;
      }
   }

   private static class RecipeMenu extends ChestMenu {
      private final AsyncChanceRecipeTask recipeTask = new AsyncChanceRecipeTask();

      public RecipeMenu(AContainer var1, Player var2, int var3) {
         super(Slimefun.getLocalization().getMessage(var2, "guide.title.main"));
         CustomMenu var4 = null;
         Optional<PlayerProfile> var5 = PlayerProfile.find(var2);
         this.setEmptySlotsClickable(false);
         this.setPlayerInventoryClickable(false);
         boolean var6 = true;
         int var7 = 31;
         ItemStack var8 = var1.getProgressBar();
         int[] var9 = var1.getInputSlots();
         int[] var10 = var1.getOutputSlots();
         if (var1 instanceof CustomRecipeMachine var11) {
            var4 = var11.getMenu();
            if (var4 != null) {
               var6 = false;
               var7 = var4.getProgressSlot();
               if (var4.getProgressBarItem() != null) {
                  var8 = var4.getProgressBarItem();
               }
            }
         } else {
            var9 = new int[]{28, 29};
            var10 = new int[]{33, 34};
         }

         BlockMenuPreset var26 = (BlockMenuPreset)Slimefun.getRegistry().getMenuPresets().get(var1.getId());
         if (var26 != null) {
            if (!var6) {
               for (Integer var13 : var26.getPresetSlots()) {
                  ItemStack var14 = var26.getItemInSlot(var13);
                  if (var14 != null) {
                     this.addItem(var13, var14, (var0, var1x, var2x, var3x) -> false);
                  }
               }
            }

            if (var6) {
               int[] var27 = new int[]{1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 22, 31, 40, 45, 46, 47, 48, 49, 50, 51, 52, 53};

               for (int var16 : var27) {
                  this.addItem(var16, ChestMenuUtils.getBackground(), (var0, var1x, var2x, var3x) -> false);
               }

               int[] var30 = new int[]{18, 19, 20, 21, 27, 30, 36, 37, 38, 39};

               for (int var17 : var30) {
                  this.addItem(var17, ChestMenuUtils.getInputSlotTexture(), (var0, var1x, var2x, var3x) -> false);
               }

               int[] var34 = new int[]{23, 24, 25, 26, 32, 35, 41, 42, 43, 44};

               for (int var18 : var34) {
                  this.addItem(var18, ChestMenuUtils.getOutputSlotTexture(), (var0, var1x, var2x, var3x) -> false);
               }

               var5.ifPresent(
                  var2x -> this.addItem(
                     0, ChestMenuUtils.getBackButton(var2, new String[]{"", "&f左键: &7返回上一页", "&fShift + 左键: &7返回主菜单"}), (var1xx, var2xx, var3x, var4x) -> {
                        // 返回按钮同样要走当前生效的指南实现（JEG），否则会被丢回原生界面
                        if (!JegBridge.back(var2x, var2x.getPlayer(), var4x.isShiftClicked())) {
                           SurvivalSlimefunGuide var5x = new SurvivalSlimefunGuide(false, false);
                           GuideHistory var6x = var2x.getGuideHistory();
                           if (var4x.isShiftClicked()) {
                              var5x.openMainMenu(var2x, var6x.getMainMenuPage());
                           } else {
                              var6x.goBack(var5x);
                           }
                        }

                        return false;
                     }
                  )
               );
            }

            List var28 = var1.getMachineRecipes();
            MachineRecipe var31 = (MachineRecipe)var28.get(var3);
            ItemStack[] var35 = var31.getInput();

            for (int var38 = 0; var38 < var35.length; var38++) {
               ItemStack var44 = var35[var38];
               if (var44 != null) {
                  this.addItem(var9[var38], var44, SingleItemRecipeGuideListener::viewItem);
               }
            }

            if (var31 instanceof CustomMachineRecipe var39) {
               int var45 = var10[0];
               List var49 = List.of(var39.getInput());
               ItemStack[] var50 = var31.getOutput();
               if (var39.isChooseOneIfHas()) {
                  ArrayList var19 = new ArrayList();

                  for (int var20 = 0; var20 < var50.length; var20++) {
                     Integer var21 = var39.getChances().get(var20);
                     ItemStack var22 = var50[var20];
                     if (var21 != null && var21 > 0 && var22 != null) {
                        var19.add(this.tagOutputChance(var22, var21));
                     }
                  }

                  this.recipeTask.add(var45, var19);
                  this.addMenuClickHandler(var45, SingleItemRecipeGuideListener::viewItem);
               } else {
                  List var51 = var39.getChances();

                  for (int var52 = 0; var52 < var50.length; var52++) {
                     int var53 = (Integer)var51.get(var52);
                     ItemStack var54 = var50[var52];
                     if (var54 != null) {
                        ItemStack var23 = var54.clone();
                        if (var53 < 100) {
                           CommonUtils.addLore(var23, true, CMIChatColor.translate("&a有&b " + var53 + "% &a的概率产出"));
                        }

                        if (var53 > 0) {
                           this.addItem(var10[var52], var23, SingleItemRecipeGuideListener::viewItem);
                        }
                     }
                  }
               }
            } else {
               for (int var40 = 0; var40 < var10.length; var40++) {
                  ItemStack var46 = var31.getOutput()[var40];
                  if (var46 != null) {
                     this.addItem(var10[var40], var46, SingleItemRecipeGuideListener::viewItem);
                  }
               }
            }

            int var41 = var31.getTicks() / 2;
            String var47 = "&e制作时间: &b" + var41 + "&es";
            if (var41 > 60) {
               var47 = var47.concat("(" + CommonUtils.formatSeconds(var41) + "&e)");
            }

            CustomItemStack var25 = new CustomItemStack(var8, var47, new String[0]);
            this.addItem(var7, var25, (var0, var1x, var2x, var3x) -> false);
         }
      }

      public void open(Player... var1) {
         super.open(var1);
         if (!this.recipeTask.isEmpty()) {
            this.recipeTask.start(this.toInventory());
         }
      }

      private ItemStack tagOutputChance(ItemStack var1, int var2) {
         var1 = var1.clone();
         CommonUtils.addLore(var1, true, CMIChatColor.translate("&a有&b " + var2 + "% &a的概率产出"));
         return var1;
      }
   }

   private static class TemplateRecipeMenu extends ChestMenu {
      private final AsyncChanceRecipeTask recipeTask = new AsyncChanceRecipeTask();

      public TemplateRecipeMenu(CustomTemplateMachine var1, Player var2, int var3, int var4) {
         super(Slimefun.getLocalization().getMessage(var2, "guide.title.main"));
         Optional<PlayerProfile> var5 = PlayerProfile.find(var2);
         this.setEmptySlotsClickable(false);
         this.setPlayerInventoryClickable(false);
         CustomMenu var6 = var1.getMenu();

         for (int var7 = 0; var7 < 54; var7++) {
            ItemStack var8 = var6.getItems().get(var7);
            if (var8 != null) {
               this.addItem(var7, var8, (var0, var1x, var2x, var3x) -> false);
            }
         }

         int var23 = var6.getSize() - 1;
         if (var23 > 0) {
            this.addItem(var23, new ItemStack(Material.AIR), (var0, var1x, var2x, var3x) -> false);
         }

         int[] var24 = var1.getInputSlots();
         int[] var9 = var1.getOutputSlots();
         int var10 = var6.getProgressSlot();
         ItemStack var11 = var6.getProgressBarItem();
         int var12 = var1.getTemplateSlot();
         MachineTemplate var13 = var1.getTemplates().get(var3);
         if (var13 != null) {
            CustomMachineRecipe var14 = var13.recipes().get(var4);
            if (var14 != null) {
               int var15 = var14.getTicks() / 2;
               ItemStack var16 = var13.template().clone();
               CommonUtils.addLore(var16, true, "&d&l&o*模板物品不消耗*");
               this.addItem(var12, var16, SingleItemRecipeGuideListener::viewItem);
               if (var24.length != 0 && var14.getInput().length != 0) {
                  for (int var17 = 0; var17 < var24.length && var17 < var14.getInput().length; var17++) {
                     ItemStack var18 = var14.getInput()[var17];
                     if (var18 != null) {
                        this.addItem(var24[var17], var18.clone(), SingleItemRecipeGuideListener::viewItem);
                     }
                  }
               }

               ItemStack[] var26 = var14.getOutput();
               if (var14.isChooseOneIfHas()) {
                  ArrayList var27 = new ArrayList();

                  for (int var19 = 0; var19 < var26.length; var19++) {
                     Integer var20 = var14.getChances().get(var19);
                     ItemStack var21 = var26[var19];
                     if (var20 != null && var20 > 0 && var21 != null) {
                        var27.add(this.tagOutputChance(var21, var20));
                     }
                  }

                  this.recipeTask.add(var9[0], var27);
                  this.addMenuClickHandler(var9[0], SingleItemRecipeGuideListener::viewItem);
               } else {
                  List var28 = var14.getChances();

                  for (int var30 = 0; var30 < var9.length; var30++) {
                     if (var30 >= var26.length) {
                        return;
                     }

                     int var31 = (Integer)var28.get(var30);
                     ItemStack var32 = var26[var30];
                     if (var32 != null) {
                        ItemStack var22 = var32.clone();
                        if (var31 < 100) {
                           CommonUtils.addLore(var22, true, CMIChatColor.translate("&a有&b " + var31 + "% &a的概率产出"));
                        }

                        if (var31 > 0) {
                           this.addItem(var9[var30], var22, SingleItemRecipeGuideListener::viewItem);
                        }
                     }
                  }
               }

               if (var10 >= 0 && var11 != null) {
                  String var29 = "&e制作时间: &b" + var15 + "&es";
                  if (var15 > 60) {
                     var29 = var29.concat("(" + CommonUtils.formatSeconds(var15) + "&e)");
                  }

                  CustomItemStack var25 = new CustomItemStack(var11, var29, new String[0]);
                  this.addItem(var10, var25, (var0, var1x, var2x, var3x) -> false);
               }
            }
         }
      }

      public void open(Player... var1) {
         super.open(var1);
         if (!this.recipeTask.isEmpty()) {
            this.recipeTask.start(this.toInventory());
         }
      }

      private ItemStack tagOutputChance(ItemStack var1, int var2) {
         var1 = var1.clone();
         CommonUtils.addLore(var1, true, CMIChatColor.translate("&a有&b " + var2 + "% &a的概率产出"));
         return var1;
      }
   }

   private static class WorkbenchRecipeMenu extends ChestMenu {
      private final AsyncChanceRecipeTask recipeTask = new AsyncChanceRecipeTask();

      public WorkbenchRecipeMenu(AContainer var1, Player var2, int var3) {
         super(Slimefun.getLocalization().getMessage(var2, "guide.title.main"));
         this.setEmptySlotsClickable(false);
         this.setPlayerInventoryClickable(false);
         int[] var6 = var1.getOutputSlots();
         CustomWorkbench var4;
         if (var1 instanceof CustomWorkbench && (var4 = (CustomWorkbench)var1).getMenu() != null) {
            BlockMenuPreset var7 = (BlockMenuPreset)Slimefun.getRegistry().getMenuPresets().get(var1.getId());

            for (Integer var9 : var7.getPresetSlots()) {
               ItemStack var10 = var7.getItemInSlot(var9);
               if (var10 != null && var10.getType() != Material.AIR) {
                  this.addItem(var9, var10, SingleItemRecipeGuideListener::viewItem);
               }
            }

            CustomLinkedMachineRecipe var22 = (CustomLinkedMachineRecipe)var4.getMachineRecipes().get(var3);
            Map<Integer, ItemStack> var23 = var22.getLinkedInput();

            for (Entry var11 : var23.entrySet()) {
               int var12 = (Integer)var11.getKey();
               this.addItem(var12, ((ItemStack)var11.getValue()).clone(), SingleItemRecipeGuideListener::viewItem);
            }

            LinkedOutput var25 = var22.getLinkedOutput();
            Map<Integer, Integer> var26 = var25.linkedChances();
            Map<Integer, ItemStack> var27 = var25.linkedOutput();

            for (Entry var14 : var27.entrySet()) {
               int var15 = (Integer)var14.getKey();
               if (var26.containsKey(var15)) {
                  int var16 = (Integer)var26.get(var15);
                  ItemStack var17 = (ItemStack)var27.get(var15);
                  if (var17 != null && var17.getType() != Material.AIR) {
                     ItemStack var18 = var17.clone();
                     if (var16 < 100) {
                        CommonUtils.addLore(var18, true, CMIChatColor.translate("&a有&b " + var16 + "% &a的概率产出"));
                     }

                     if (var16 > 0) {
                        this.addItem(var15, var18, SingleItemRecipeGuideListener::viewItem);
                     }
                  }
               } else {
                  this.addItem(var15, ((ItemStack)var27.get(var15)).clone(), SingleItemRecipeGuideListener::viewItem);
               }
            }

            for (ItemStack var31 : var25.freeOutput()) {
               for (int var20 : var6) {
                  ItemStack var21 = this.getItemInSlot(var20);
                  if (var21 == null || var21.getType() == Material.AIR) {
                     this.addItem(var20, var31.clone(), SingleItemRecipeGuideListener::viewItem);
                     break;
                  }
               }
            }
         }
      }

      public void open(Player... var1) {
         super.open(var1);
         if (!this.recipeTask.isEmpty()) {
            this.recipeTask.start(this.toInventory());
         }
      }

      private ItemStack tagOutputChance(ItemStack var1, int var2) {
         var1 = var1.clone();
         CommonUtils.addLore(var1, true, CMIChatColor.translate("&a有&b " + var2 + "% &a的概率产出"));
         return var1;
      }
   }
}
