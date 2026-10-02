package tech.komutech.objects;

import java.util.Random;
import javax.annotation.Nonnull;

public record Range(int min, int max) {
   public int getRandomBetween(@Nonnull Random var1) {
      return var1.nextInt(this.min, this.max + 1);
   }

   public int getDistance() {
      return this.max - this.min;
   }
}
