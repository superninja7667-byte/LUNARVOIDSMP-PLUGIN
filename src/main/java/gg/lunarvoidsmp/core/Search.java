package gg.lunarvoidsmp.core;

import java.util.Locale;

public final class Search {
    private Search() {}
    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("[^\\p{L}\\p{N} ]", "").trim();
    }
    /** Negative means no reasonable match; smaller nonnegative ranks are better. */
    public static int rank(String query, String name) {
        String q = normalize(query), n = normalize(name);
        if (q.isEmpty() || n.equals(q)) return 0;
        if (n.startsWith(q)) return 1;
        if (n.contains(q)) return 2;
        if (q.length() < 3 || q.length() > 64) return -1;
        int best = distance(q, n);
        for (String word : n.split(" +")) best = Math.min(best, distance(q, word));
        return best <= Math.min(2, q.length() / 3) ? 3 + best : -1;
    }
    private static int distance(String a, String b) {
        int[] row = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) row[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int diagonal = row[0]; row[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int old = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                diagonal = old;
            }
        }
        return row[b.length()];
    }
}
