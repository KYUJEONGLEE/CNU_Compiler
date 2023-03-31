package prettyprinter;

public class Test {
	public static int sum(int var0) {
		int var1 = 0;

		do {
			var1 += var0;
			--var0;
		} while (var0 != 0);

		return var1;
	}

	public static void main(String[] var0) {
		byte var1 = 100;
		int var2 = sum(var1);
		System.out.println(var2);
	}
}