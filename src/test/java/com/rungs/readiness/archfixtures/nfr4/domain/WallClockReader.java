package com.rungs.readiness.archfixtures.nfr4.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.chrono.JapaneseDate;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/** NFR-4 violation fixture: wall-clock reads and randomness in the domain, called directly and by reference. */
public final class WallClockReader {

    public long instant() {
        return Instant.now().getEpochSecond();
    }

    public int zonedDate() {
        return LocalDate.now(ZoneOffset.UTC).getYear();
    }

    public int chronoDate() {
        return JapaneseDate.now().lengthOfMonth();
    }

    public long millis() {
        return System.currentTimeMillis();
    }

    public long nanos() {
        return System.nanoTime();
    }

    public int systemClockDate() {
        return LocalDate.now(Clock.systemDefaultZone()).getYear();
    }

    public Clock utcClock() {
        return Clock.systemUTC();
    }

    public Clock zonedClock() {
        return Clock.system(ZoneOffset.UTC);
    }

    public int random() {
        return new Random().nextInt();
    }

    public int threadLocalRandom() {
        return ThreadLocalRandom.current().nextInt();
    }

    public int secureRandom() {
        return new SecureRandom().nextInt();
    }

    public int splittable() {
        return new SplittableRandom().nextInt();
    }

    public int defaultGenerator() {
        return RandomGenerator.getDefault().nextInt();
    }

    public int namedGenerator() {
        return RandomGenerator.of("L64X128MixRandom").nextInt();
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public Supplier<Instant> instantReference() {
        return Instant::now;
    }

    public LongSupplier millisReference() {
        return System::currentTimeMillis;
    }

    public LongSupplier nanosReference() {
        return System::nanoTime;
    }

    public Supplier<Clock> clockReference() {
        return Clock::systemUTC;
    }

    public Supplier<UUID> uuidReference() {
        return UUID::randomUUID;
    }

    /** Reading an injected clock is deterministic, so it is allowed. */
    public int injectedDate(Clock clock) {
        return LocalDate.now(clock).getYear();
    }
}
