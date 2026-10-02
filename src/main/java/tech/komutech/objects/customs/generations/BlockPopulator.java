package tech.komutech.objects.customs.generations;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import java.net.URL;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.meta.SkullMeta;
import tech.komutech.KT;
import tech.komutech.objects.Range;

public class BlockPopulator extends org.bukkit.generator.BlockPopulator {
   private static final List<String> blockedWorlds = List.of(
      "CAsteroidBelt",
      "CMars",
      "CMoon",
      "dimensionalhome",
      "ft_world",
      "ne_muspelheim",
      "ne_niflheim",
      "SmallSpace",
      "space",
      "world_galactifun_earth_orbit",
      "world_galactifun_enceladus",
      "world_galactifun_europa",
      "world_galactifun_io",
      "world_galactifun_mars",
      "world_galactifun_the_moon",
      "world_galactifun_titan",
      "world_galactifun_venus",
      "world_void",
      "corporate_dimension",
      "logispace"
   );

   public void populate(@Nonnull World var1, @Nonnull Random var2, @Nonnull Chunk var3) {
      if (!blockedWorlds.contains(var1.getName())) {
         // 原实现遍历所有附属的世代表；单插件形态下只有本插件一份，改为全局静态表
         for (GenerationInfo var9 : KT.generations) {
            for (GenerationArea var12 : var9.getAreas()) {
               if (var12.getEnvironment() == var1.getEnvironment()) {
                  for (int var13 = 0; var13 < var12.getAmount(); var13++) {
                     this.generateNext(var3.getX(), var3.getZ(), var1, var2, var9, var12);
                  }
               }
            }
         }
      }
   }

   private void generateNext(int var1, int var2, @Nonnull World var3, @Nonnull Random var4, @Nonnull GenerationInfo var5, @Nonnull GenerationArea var6) {
      Range var12 = var6.getHeight();
      int var13 = var12.getDistance() + 1;
      if (var13 < 0) {
         var13 = 1;
      }

      int var7;
      double var8;
      double var10;
      if ((var10 = var4.nextDouble(0.0, var13)) < (var8 = var12.max() - var6.getMost() + 1)) {
         int var14 = (int)(var10 * 2.0);
         var7 = var12.max() - var14;
      } else {
         double var27;
         int var28 = (int)((var27 = var10 - var8) * 2.0);
         var7 = var12.min() + var28;
      }

      int var29 = (var1 << 4) + var4.nextInt(16);
      int var15 = var7;
      int var16 = (var2 << 4) + var4.nextInt(16);

      for (int var17 = 0; var17 < var6.getSize().getRandomBetween(var4); var17++) {
         Location var22 = new Location(var3, var29, var15, var16);
         Block var23 = var3.getBlockAt(var29, var15, var16);
         if (var29 < var1 << 4 || var29 >= (var1 << 4) + 16 || var16 < var2 << 4 || var16 >= (var2 << 4) + 16 || var23.getType() != var6.getReplacement()) {
            break;
         }

         SlimefunItemStack var24 = var5.getSlimefunItemStack();
         var23.setType(var24.getType(), false);
         URL var19;
         PlayerProfile var21;
         if (var24.getType() == Material.PLAYER_HEAD
            && (var21 = ((SkullMeta)var24.getItemMeta()).getPlayerProfile()) != null
            && (var19 = var21.getTextures().getSkin()) != null) {
            PlayerHead.setSkin(var23, PlayerSkin.fromURL(var19.toString()), false);
         }

         BlockDataController var25 = Slimefun.getDatabaseManager().getBlockDataController();
         var25.createBlock(var22, var5.getSlimefunItemStack().getItemId());
         var7 = var4.nextInt(0, 3);
         if (var7 == 0) {
            var29++;
         } else if (var7 == 1) {
            var15++;
         } else if (var7 == 2) {
            var16++;
         }
      }
   }
}
