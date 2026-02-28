package me.naimad.fastregex.bench;

import me.naimad.fastregex.FastRegex;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(value = 2, jvmArgsAppend = {
        "-Djava.library.path=dist" // Path to fastregex.dll / libfastregex.so
    })
@State(Scope.Benchmark)
public class RegexBench {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RegexBench.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    // Patterns supported by both engines
    @Param({
            "^[A-Za-z0-9_]{3,16}$",
            "^(?:GET|POST)\\s+/[A-Za-z0-9/_-]{1,64}\\s+HTTP/1\\.[01]$",
            "^[^@\\s]{1,64}@[^@\\s]{1,255}$"
    })
    public String regex;

    // Test data batch size
    @Param({"64", "512"})
    public int n;

    public String[] batch;

    // JDK
    private Pattern jdkPattern;

    // FastRegex
    private long handle;
    private FastRegex.PackedUtf8 packed;
    private long[] outBits;

    @Setup(Level.Trial)
    public void setup() {
        batch = new String[n];
        for (int i = 0; i < n; i++) {
            // Mix matching and non-matching strings
            batch[i] = (i % 2 == 0) ? makeMatching(i) : makeNonMatching(i);
        }

        jdkPattern = Pattern.compile(regex);

        handle = FastRegex.compile(regex);
        packed = FastRegex.packUtf8Direct(batch);
        outBits = new long[(n + 63) / 64];
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        FastRegex.release(handle);
    }

    private String makeMatching(int i) {
        // Keep it simple and compatible with patterns
        return "User_" + (1000 + i);
    }

    private String makeNonMatching(int i) {
        return "!!!" + i + "!!!";
    }

    // --- Benchmarks ---

    @Benchmark
    public void jdk_matches_loop(Blackhole bh) {
        int count = 0;
        for (String s : batch) {
            if (jdkPattern.matcher(s).matches()) count++;
        }
        bh.consume(count);
    }

    @Benchmark
    public void fastregex_match_only(Blackhole bh) {
        // Reuse 'packed' from setup
        FastRegex.batchMatchesUtf8Direct(handle, packed.data, packed.offsets, packed.lengths, outBits);

        int count = 0;
        for (int i = 0; i < n; i++) {
            if (FastRegex.getBit(outBits, i)) count++;
        }
        bh.consume(count);
    }

    @Benchmark
    public void fastregex_pack_and_match(Blackhole bh) {
        FastRegex.PackedUtf8 p = FastRegex.packUtf8Direct(batch);
        long[] bits = new long[(n + 63) / 64];

        FastRegex.batchMatchesUtf8Direct(handle, p.data, p.offsets, p.lengths, bits);

        int count = 0;
        for (int i = 0; i < n; i++) {
            if (FastRegex.getBit(bits, i)) count++;
        }
        bh.consume(count);
    }
}