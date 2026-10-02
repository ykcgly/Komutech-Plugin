package tech.komutech.objects.script.lambda;

@FunctionalInterface
public interface CiConsumer<A, B, C> {
   void accept(A var1, B var2, C var3);
}
