package comp0012.target;

public class ConstantVariableFolding {
    public int optimiseMe() {
        // a <- 534245
        int a = 534245;

        // b <- 533011
        int b = a - 1234;

        // <- 4603228141.221259
        System.out.println((120298345 - a) * 38.435792873);

        for (int i = 0; i < 10; i++) {
            System.out.println((b - a) * i);
        }

        // -> 284758461695
        return a * b;
    }

// @formatter:off

    public int methodOne(){
        int a = 62;                 // a <- 62
        int b = (a + 764) * 3;      // b <- 2478
        return b + 1234 - a;        // -> 3650
    }

    public double methodTwo(){
        double i = 0.67;            // i <- 0.67
        int j = 1;                  // j <- 1
        return i + j;               // -> 1.67
    }

    public boolean methodThree(){
        int x = 12345;              // x <- 12345
        int y = 54321;              // y <- 54321
        return x > y;               // -> False
    }

    public boolean methodFour(){
        long x = 4835783423L;       // x <- 4835783423
        long y = 400000;            // y <- 400000
        long z = x + y;             // z <- 4836183423
        return x > y;               // -> True
    }

}
// @formatter:on
