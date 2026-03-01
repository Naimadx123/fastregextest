package me.naimad.fastregex;

public class FastRegexCLI {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -Djava.library.path=libs -jar fastregex.jar <regex> <string>");
            return;
        }

        String regex = args[0];
        String testString = args[1];

        try {
            long handle = FastRegex.compile(regex);
            String[] batch = { testString };
            FastRegex.PackedUtf8 p = FastRegex.packUtf8Direct(batch);
            long[] bits = new long[1];
            FastRegex.batchMatchesUtf8Direct(handle, p.data, p.offsets, p.lengths, bits);
            boolean matches = FastRegex.getBit(bits, 0);

            System.out.println("Regex: " + regex);
            System.out.println("String: " + testString);
            System.out.println("Matches: " + matches);
            FastRegex.release(handle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
