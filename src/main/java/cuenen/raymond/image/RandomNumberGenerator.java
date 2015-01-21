package cuenen.raymond.image;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A linear congruential generator (LCG). More specifically a (Derrick Henry
 * "Dick") Lehmer random number generator (RNG) with particular parameters
 * suggested by Stephen K. Park and Keith W. Miller, now known as MINSTD.
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class RandomNumberGenerator {

    /**
     * The Euclidean algorithm to determine the greatest common divisor (GCD)
     * between two natural numbers.
     *
     * @param a The first natural number.
     * @param b The second natural number.
     * @return The greatest common divisor.
     */
    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * The multiplier, <code>7&#x2075;</code>, of the RNG's recurrence relation.
     */
    public static final long Ar = 16807L;

    /**
     * The modulus, <code>2&sup3;&sup1;</code> - 1, of the RNG's recurrence
     * relation.
     */
    public static final long Pr = 2147483647L;

    /**
     * Define the integer part, <code>&lfloor;Pr / Ar&rfloor;</code>, of the
     * RNG's modulus divided by the RNG's multiplier to use the trick of Linus
     * Schrage.
     */
    private static final long QL = Pr / Ar;

    /**
     * Define the remainder, <code>Pr mod Ar</code>, of the RNG's modules
     * divided by the RNG's multiplier to use the trick of Linus Schrage.
     */
    private static final long RR = Pr % Ar;

    /**
     * The seed of the RNG's recurrence relation.
     */
    private final AtomicLong seed = new AtomicLong();

    private static long seed(long seed) {
        return gcd(seed, Pr) > 1 ? seed((seed + 1) % Pr) : seed;
    }

    /**
     * RamdomNumberGenerator.
     */
    public RandomNumberGenerator() {
        this(System.currentTimeMillis() % Pr);
    }

    /**
     * RandomNumberGenerator.
     *
     * @param seed The initial seed value.
     */
    public RandomNumberGenerator(long seed) {
        this.seed.set(seed(seed));
    }

    /**
     * Generate a random number between x and y.
     *
     * @param x The lower bound.
     * @param y The upper bound.
     * @return The generated pseudorandom number.
     */
    public double random(double x, double y) {
        final long l = seed.get() / QL;
        final long s = seed.updateAndGet((c) -> {
            final long n = Ar * (c - QL * l) - RR * l;
            return n < 0 ? n + Pr : n;
        });
        return x + (y - x) * (s - 1) / (Pr - 1);
    }

    /**
     * Generate a random number between 0 and 1.
     *
     * @return The generated pseudorandom number.
     */
    public double random() {
        return random(0, 1);
    }

    /**
     * Returns the current seed of this RNG.
     *
     * @return The current seed.
     */
    public long seed() {
        return seed.get();
    }
}
