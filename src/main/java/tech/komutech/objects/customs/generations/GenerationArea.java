package tech.komutech.objects.customs.generations;

import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import tech.komutech.objects.Range;

public class GenerationArea {
   private Range height;
   private int most;
   private int amount;
   private Range size;
   private Material replacement;
   private Environment environment;

   public GenerationArea(@Nonnull Range var1, int var2, int var3, @Nonnull Range var4, @Nonnull Material var5, @Nonnull Environment var6) {
      this.height = var1;
      this.most = var2;
      this.amount = var3;
      if (this.amount > 200) {
         this.amount = 200;
      }

      this.size = var4;
      this.replacement = var5;
      this.environment = var6;
   }

   public GenerationArea(@Nonnull Range var1, int var2, int var3, @Nonnull Range var4) {
      this(var1, var2, var3, var4, Material.STONE, Environment.NORMAL);
   }

   public Range getHeight() {
      return this.height;
   }

   public int getMost() {
      return this.most;
   }

   public int getAmount() {
      return this.amount;
   }

   public Range getSize() {
      return this.size;
   }

   public Material getReplacement() {
      return this.replacement;
   }

   public Environment getEnvironment() {
      return this.environment;
   }

   public void setHeight(Range var1) {
      this.height = var1;
   }

   public void setMost(int var1) {
      this.most = var1;
   }

   public void setAmount(int var1) {
      this.amount = var1;
   }

   public void setSize(Range var1) {
      this.size = var1;
   }

   public void setReplacement(Material var1) {
      this.replacement = var1;
   }

   public void setEnvironment(Environment var1) {
      this.environment = var1;
   }
}
