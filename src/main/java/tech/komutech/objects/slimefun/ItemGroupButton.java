package tech.komutech.objects.slimefun;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.Optional;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tech.komutech.script.ScriptEval;
import tech.komutech.util.colors.CMIChatColor;

import tech.komutech.objects.script.ban.CommandSafe;
import tech.komutech.load.ScriptLoader;
import tech.komutech.util.ExceptionHandler;

public class ItemGroupButton extends SubItemGroup {
   private final List<String> actions;

   public ItemGroupButton(NamespacedKey var1, NestedItemGroup var2, ItemStack var4, int var5, @Nullable List<String> var6) {
      super(var1, var2, var4, var5);
      this.actions = var6;
   }

   public void run(Player var1, int var2, ItemStack var3, ClickAction var4, SlimefunGuideMode var5) {
      if (this.actions != null) {
         for (String var7 : this.actions) {
            if (var7.split(" ").length < 2) {
               ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现未知的操作格式: " + var7);
            } else {
               String var8 = var7.split(" ")[0];
               String var9 = var7.split(" ")[1];
               switch (var8) {
                  case "link":
                     var1.sendMessage(CMIChatColor.translate("&e单击此处: "));
                     TextComponent var24 = new TextComponent(var9);
                     var24.setColor(ChatColor.GRAY);
                     ClickEvent var26 = new ClickEvent(Action.OPEN_URL, var9);
                     var24.setClickEvent(var26);
                     var1.sendMessage(var24);
                     break;
                  case "console":
                     // 用完整命令串（而非第二个 token）做校验：CommandSafe 内部会取首段并剥离
                     // namespace: 前缀，避免 minecraft:op 这类写法绕过黑名单
                     String consoleCmd = var7.trim().substring(var8.length()).trim();
                     if (CommandSafe.isBadCommand(consoleCmd)) {
                        ExceptionHandler.handleDanger("在" + this.getKey().getKey() + "物品组按钮中发现执行服务器高危操作,请联系附属对应作者进行处理！！！");
                     } else if (!var1.isOp() && !var1.hasPermission("komutech.console")) {
                        // 控制台命令以服务器身份执行，必须限权，否则任意玩家点击即可触发
                        var1.sendMessage(CMIChatColor.translate("&c你没有权限执行该操作"));
                     } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd.replaceAll("%player%", var1.getName()));
                     }
                     break;
                  case "open_itemgroup":
                     if (var9.split(":").length < 2) {
                        ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现未知的物品组 NamespacedKey: " + var9);
                     } else {
                        String var25 = var9.split(":")[0];
                        String var27 = var9.split(":")[1];
                        int var28 = 1;
                        if (var9.split(":").length > 2) {
                           try {
                              var28 = Integer.parseInt(var9.split(":")[2]);
                           } catch (NumberFormatException var20) {
                           }
                        }

                        Optional var23;
                        if ((var23 = PlayerProfile.find(var1)).isEmpty()) {
                           ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现无法获取 PlayerProfile: " + var1.getName());
                        } else {
                           PlayerProfile var16 = (PlayerProfile)var23.get();

                           for (ItemGroup var18 : Slimefun.getRegistry().getAllItemGroups()) {
                              if (var18.getKey().getNamespace().equals(var25) && var18.getKey().getKey().equals(var27)) {
                                 SlimefunGuideImplementation var19 = Slimefun.getRegistry().getSlimefunGuide(var5);
                                 var19.openItemGroup(var16, var18, var28);
                              }
                           }
                           break;
                        }
                     }
                     break;
                  case "display_slimefunitem":
                     Optional var22 = PlayerProfile.find(var1);
                     if (var22.isEmpty()) {
                        ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现无法获取 PlayerProfile: " + var1.getName());
                     } else {
                        SlimefunItem var13 = SlimefunItem.getById(var9);
                        if (var13 == null) {
                           ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现未知的 SlimefunItem ID: " + var9);
                        } else {
                           PlayerProfile var14 = (PlayerProfile)var22.get();
                           SlimefunGuideImplementation var15 = Slimefun.getRegistry().getSlimefunGuide(var5);
                           var15.displayItem(var14, var13, true);
                        }
                     }
                     break;
                  case "script":
                     ScriptEval var12 = ScriptLoader.load(var9, "物品组按钮", this.getKey().getKey());
                     if (var12 != null) {
                        var12.evalFunction("onButtonGroupClick", new Object[]{var1, var2, var3, var4, var5});
                     }
                     break;
                  default:
                     ExceptionHandler.handleWarning("在" + this.getKey().getKey() + "物品组按钮中发现未知的操作类型: " + var7);
               }
            }
         }
      }
   }
}
