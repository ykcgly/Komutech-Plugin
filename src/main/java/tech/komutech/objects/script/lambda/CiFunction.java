package tech.komutech.objects.script.lambda;

@FunctionalInterface
public interface CiFunction<A, B, C, R> {
   R apply(A var1, B var2, C var3);
}
