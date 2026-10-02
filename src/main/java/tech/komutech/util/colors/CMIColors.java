package tech.komutech.util.colors;

import java.awt.Color;
import org.bukkit.Material;

public enum CMIColors {
   White(0, "White", Material.WHITE_DYE, new Color(249, 255, 254)),
   Orange(1, "Orange", Material.ORANGE_DYE, new Color(249, 128, 29)),
   Magenta(2, "Magenta", Material.MAGENTA_DYE, new Color(199, 78, 189)),
   Light_Blue(3, "Light Blue", Material.LIGHT_BLUE_DYE, new Color(58, 179, 218)),
   Yellow(4, "Yellow", Material.YELLOW_DYE, new Color(254, 216, 61)),
   Lime(5, "Lime", Material.LIME_DYE, new Color(128, 199, 31)),
   Pink(6, "Pink", Material.PINK_DYE, new Color(243, 139, 170)),
   Gray(7, "Gray", Material.GRAY_DYE, new Color(71, 79, 82)),
   Light_Gray(8, "Light Gray", Material.LIGHT_GRAY_DYE, new Color(157, 157, 151)),
   Cyan(9, "Cyan", Material.CYAN_DYE, new Color(22, 156, 156)),
   Purple(10, "Purple", Material.PURPLE_DYE, new Color(137, 50, 184)),
   Blue(11, "Blue", Material.BLUE_DYE, new Color(60, 68, 170)),
   Brown(12, "Brown", Material.BROWN_DYE, new Color(131, 84, 50)),
   Green(13, "Green", Material.GREEN_DYE, new Color(94, 124, 22)),
   Red(14, "Red", Material.RED_DYE, new Color(176, 46, 38)),
   Black(15, "Black", Material.BLACK_DYE, new Color(29, 29, 33));

   private final int id;
   private final String name;
   private final Material material;
   private Color color;

   private CMIColors(int nullxx, String nullxxx, Material nullxxxx, Color nullxxxxx) {
      this.id = nullxx;
      this.name = nullxxx;
      this.material = nullxxxx;
      this.color = nullxxxxx;
   }

   private CMIColors(int nullxx, String nullxxx, Material nullxxxx) {
      this.id = nullxx;
      this.name = nullxxx;
      this.material = nullxxxx;
   }

   public static CMIColors getById(int var0) {
      for (CMIColors var4 : values()) {
         if (var4.getId() == var0) {
            return var4;
         }
      }

      return White;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Material getMaterial() {
      return this.material;
   }

   public Color getColor() {
      return this.color;
   }
}
