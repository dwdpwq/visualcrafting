package com.visualcrafting.trade;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * v5 §2.5.1 种子化 ID 稳定性测试（独立测试副本，不接入主代码，零第三方依赖）。
 *
 * <p>验证对象：v5 §2.1 / §2.4 定义的种子化 ID 生成规则：
 * <pre>
 *   默认 hash  = sha256(profId + "\u0000" + level + "\u0000" + canonicalOfferJson)[:32]
 *   默认 ID    = UUID.nameUUIDFromBytes(hashHex UTF-8 bytes)
 *   冲突解歧   = 仅当同一等级内 canonicalOfferJson 完全相同时，
 *               UUID.nameUUIDFromBytes((hashHex + "#" + occurrenceIndex) UTF-8 bytes)
 * </pre>
 *
 * <p>性质：纯算法确定性验证（无 MC / 无 JUnit 依赖），每种场景至少 10 次/条验证确定性。
 * 运行方式：java -cp &lt;classes&gt; com.visualcrafting.trade.TradeIdStabilityTest [csv输出路径]
 * 全部断言通过打印 PASS；任一失败打印 FAIL 并 System.exit(1)。
 * 跨 JVM 重启一致性属外部调度范畴（人工/CI 两次独立进程对比 CSV），
 * 说明见 temp/CrossJvmStabilityCheck.md，不在本测试内执行。
 */
public class TradeIdStabilityTest {

    private static int failures = 0;
    private static final List<String> LOG = new ArrayList<>();

    private static void check(boolean cond, String msg) {
        if (cond) {
            LOG.add("PASS " + msg);
        } else {
            failures++;
            LOG.add("FAIL " + msg);
            System.out.println("[FAIL] " + msg);
        }
    }

    /** 与 v5 §2.1/§2.4 对齐的种子化 ID 规则实现（独立测试副本，勿与主代码混淆）。 */
    static final class Seeds {
        static String hashOf(String profId, int level, String canonicalOfferJson) {
            String input = profId + "\u0000" + level + "\u0000" + canonicalOfferJson;
            byte[] digest = sha256(input.getBytes(StandardCharsets.UTF_8));
            String hex = toHex(digest);
            return hex.substring(0, 32); // [:32] 截断
        }

        static UUID defaultId(String profId, int level, String canonicalOfferJson) {
            return UUID.nameUUIDFromBytes(
                    hashOf(profId, level, canonicalOfferJson).getBytes(StandardCharsets.UTF_8));
        }

        static UUID disambiguatedId(String profId, int level, String canonicalOfferJson, int occurrenceIndex) {
            String hash = hashOf(profId, level, canonicalOfferJson);
            return UUID.nameUUIDFromBytes((hash + "#" + occurrenceIndex).getBytes(StandardCharsets.UTF_8));
        }

        private static byte[] sha256(byte[] in) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(in);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String toHex(byte[] b) {
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        }
    }

    /** 极简 JSON 规范化器（v5 §2.4：键字典序、数值规范化、null 空字段省略、无空白）。 */
    static final class Canon {
        static String canonicalize(String json) {
            Parser p = new Parser(json);
            Object v = p.parseValue();
            p.skipWs();
            if (p.pos != p.src.length()) {
                throw new IllegalArgumentException("trailing content at " + p.pos);
            }
            return serialize(v);
        }

        static String serialize(Object v) {
            if (v == null) return "null";
            if (v instanceof String s) return quote(s);
            if (v instanceof Boolean || v instanceof Integer || v instanceof Long) return String.valueOf(v);
            if (v instanceof BigDecimal d) return normalizeNumber(d);
            if (v instanceof TreeMap<?, ?> m) {
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (java.util.Map.Entry<?, ?> e : m.entrySet()) {
                    Object val = e.getValue();
                    if (val == null) continue; // §2.4：空字段省略，不写 null
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(quote((String) e.getKey())).append(':').append(serialize(val));
                }
                return sb.append('}').toString();
            }
            if (v instanceof List<?> l) {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (Object o : l) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(serialize(o));
                }
                return sb.append(']').toString();
            }
            throw new IllegalStateException("unexpected type " + v.getClass());
        }

        private static String normalizeNumber(BigDecimal d) {
            return d.stripTrailingZeros().toPlainString();
        }

        private static String quote(String s) {
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.append('"').toString();
        }

        static final class Parser {
            final String src;
            int pos = 0;

            Parser(String src) { this.src = src; }

            Object parseValue() {
                skipWs();
                char c = src.charAt(pos);
                return switch (c) {
                    case '{' -> parseObject();
                    case '[' -> parseArray();
                    case '"' -> parseString();
                    case 't' -> { expect("true"); yield Boolean.TRUE; }
                    case 'f' -> { expect("false"); yield Boolean.FALSE; }
                    case 'n' -> { expect("null"); yield null; }
                    default -> parseNumber();
                };
            }

            TreeMap<String, Object> parseObject() {
                expect("{");
                TreeMap<String, Object> m = new TreeMap<>();
                skipWs();
                if (peek() == '}') { pos++; return m; }
                while (true) {
                    skipWs();
                    String k = parseString();
                    skipWs();
                    expect(":");
                    Object v = parseValue();
                    m.put(k, v);
                    skipWs();
                    char c = src.charAt(pos++);
                    if (c == '}') return m;
                    if (c != ',') throw new IllegalArgumentException("expected , or } at " + (pos - 1));
                }
            }

            List<Object> parseArray() {
                expect("[");
                List<Object> l = new ArrayList<>();
                skipWs();
                if (peek() == ']') { pos++; return l; }
                while (true) {
                    l.add(parseValue());
                    skipWs();
                    char c = src.charAt(pos++);
                    if (c == ']') return l;
                    if (c != ',') throw new IllegalArgumentException("expected , or ] at " + (pos - 1));
                }
            }

            String parseString() {
                expect("\"");
                StringBuilder sb = new StringBuilder();
                while (true) {
                    char c = src.charAt(pos++);
                    if (c == '"') return sb.toString();
                    if (c == '\\') {
                        char e = src.charAt(pos++);
                        if (e == 'u') {
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                        } else {
                            switch (e) {
                                case '"' -> sb.append('"');
                                case '\\' -> sb.append('\\');
                                case '/' -> sb.append('/');
                                case 'b' -> sb.append('\b');
                                case 'f' -> sb.append('\f');
                                case 'n' -> sb.append('\n');
                                case 'r' -> sb.append('\r');
                                case 't' -> sb.append('\t');
                                default -> throw new IllegalArgumentException("bad escape \\" + e);
                            }
                        }
                    } else {
                        sb.append(c);
                    }
                }
            }

            Object parseNumber() {
                int start = pos;
                while (pos < src.length()) {
                    char c = src.charAt(pos);
                    if (c == ',' || c == '}' || c == ']' || c == ' ' || c == '\t' || c == '\n' || c == '\r') break;
                    pos++;
                }
                String t = src.substring(start, pos).trim();
                if (t.isEmpty()) throw new IllegalArgumentException("empty number at " + start);
                BigDecimal d = new BigDecimal(t);
                if (d.stripTrailingZeros().scale() <= 0) {
                    try { return Integer.valueOf(d.toBigIntegerExact().intValueExact()); } catch (ArithmeticException ignore) { /* fall through */ }
                }
                return d;
            }

            void skipWs() {
                while (pos < src.length()) {
                    char c = src.charAt(pos);
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                    else break;
                }
            }

            char peek() { return src.charAt(pos); }

            void expect(String s) {
                if (!src.startsWith(s, pos)) throw new IllegalArgumentException("expected " + s + " at " + pos);
                pos += s.length();
            }
        }
    }

    // ------------------------------------------------------------------
    // 固定样本（与 v5 §3.2 snapshot 示例同源的 canonical 内容）
    // ------------------------------------------------------------------
    static final String PROF_LIBRARIAN = "minecraft:librarian";
    static final String PROF_FARMER = "minecraft:farmer";
    static final int LEVEL_3 = 3;
    static final int LEVEL_4 = 4;
    static final String CANON_PAPER_BOOK =
            "{\"cost1\":\"minecraft:paper\",\"cost1Count\":24,\"result\":\"minecraft:book\",\"resultCount\":1,\"maxUses\":12,\"xp\":2,\"priceMultiplier\":0.05}";

    public static void main(String[] args) {
        String csvOut = args.length > 0 ? args[0] : "temp/trade_id_stability_report.csv";
        System.out.println("[TradeIdStabilityTest] start");
        runAllScenarios();
        writeCsv(csvOut);
        System.out.println("[TradeIdStabilityTest] passed=" + LOG.size() + " failed=" + failures);
        if (failures == 0) {
            System.out.println("[TradeIdStabilityTest] RESULT: PASS");
        } else {
            System.out.println("[TradeIdStabilityTest] RESULT: FAIL");
            System.exit(1);
        }
    }

    static void runAllScenarios() {
        // S1: 相同输入 → 相同默认 UUID（10 次）
        UUID defBase = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
        for (int i = 0; i < 10; i++) {
            UUID u = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
            check(u.equals(defBase), "S1 default deterministic run " + (i + 1));
        }

        // S2: level 参与种子 → 不同 level 必不同 UUID
        UUID idL4 = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_4, CANON_PAPER_BOOK);
        for (int i = 0; i < 10; i++) {
            check(!defBase.equals(idL4) && !defBase.equals(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_4, CANON_PAPER_BOOK)),
                    "S2 level sensitivity run " + (i + 1));
        }

        // S3: profId 参与种子 → 不同职业必不同 UUID
        UUID farm = Seeds.defaultId(PROF_FARMER, LEVEL_3, CANON_PAPER_BOOK);
        for (int i = 0; i < 10; i++) {
            check(!defBase.equals(farm) && !defBase.equals(Seeds.defaultId(PROF_FARMER, LEVEL_3, CANON_PAPER_BOOK)),
                    "S3 profession sensitivity run " + (i + 1));
        }

        // S4: canonicalOfferJson 参与种子 → 内容不同必不同 UUID
        String other = CANON_PAPER_BOOK.replace("\"result\":\"minecraft:book\"", "\"result\":\"minecraft:enchanted_book\"");
        for (int i = 0; i < 10; i++) {
            check(!defBase.equals(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, other)),
                    "S4 content sensitivity run " + (i + 1));
        }

        // S5: canonical 键序无关 → 同语义不同键序 → 同 canonical → 同 UUID
        String ca = Canon.canonicalize("{\"b\":1,\"a\":2}");
        String cb = Canon.canonicalize("{\"a\":2,\"b\":1}");
        check(ca.equals(cb), "S5 canonical key order equal (" + ca + ")");
        for (int i = 0; i < 10; i++) {
            check(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, ca).equals(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, cb)),
                    "S5 canonical key order same id run " + (i + 1));
        }

        // S6: 同内容同 occurrenceIndex → 解歧 UUID 稳定（10 次）
        UUID disBase = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, 3);
        for (int i = 0; i < 10; i++) {
            UUID u = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, 3);
            check(u.equals(disBase), "S6 disambiguated deterministic run " + (i + 1));
        }

        // S7: 同内容不同 occurrenceIndex → 解歧 UUID 互不相同
        UUID[] ids = new UUID[5];
        for (int idx = 0; idx < 5; idx++) {
            ids[idx] = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, idx);
        }
        boolean spread = true;
        for (int x = 0; x < ids.length; x++) {
            for (int y = x + 1; y < ids.length; y++) {
                spread &= !ids[x].equals(ids[y]);
            }
        }
        check(spread, "S7 occurrence spread distinct");

        // S8: 解歧 ID 必须与默认 ID 不同
        for (int i = 0; i < 10; i++) {
            check(!defBase.equals(Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, 0)),
                    "S8 disambiguated differs from default run " + (i + 1));
        }

        // S9: 默认 hash 截断为 32 字符 hex（v5 §2.1 [:32]）
        String hash = Seeds.hashOf(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
        check(hash.length() == 32 && hash.matches("[0-9a-f]{32}"), "S9 hash is 32 lowercase hex chars (" + hash + ")");

        // S10: null 空字段省略（§2.4）→ 同 canonical → 同 UUID
        String withNull = "{\"result\":\"minecraft:book\",\"resultCount\":1,\"cost2\":null}";
        String without = "{\"result\":\"minecraft:book\",\"resultCount\":1}";
        check(Canon.canonicalize(withNull).equals(Canon.canonicalize(without)), "S10 null field omitted");
        for (int i = 0; i < 10; i++) {
            check(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, Canon.canonicalize(withNull))
                            .equals(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, Canon.canonicalize(without))),
                    "S10 null field same id run " + (i + 1));
        }

        // S11: 数值规范化（§2.4）→ 1.0 与 1 同 canonical → 同 UUID
        String d1 = "{\"n\":1.0}";
        String d2 = "{\"n\":1}";
        check(Canon.canonicalize(d1).equals(Canon.canonicalize(d2)), "S11 numeric normalized");
        for (int i = 0; i < 10; i++) {
            check(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, Canon.canonicalize(d1))
                            .equals(Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, Canon.canonicalize(d2))),
                    "S11 numeric same id run " + (i + 1));
        }

        // S12: 键序不同 + 同 occurrenceIndex → 解歧 UUID 一致
        String a12 = Canon.canonicalize("{\"result\":\"minecraft:book\",\"resultCount\":1,\"cost1\":\"minecraft:paper\",\"cost1Count\":24}");
        String b12 = Canon.canonicalize("{\"cost1Count\":24,\"cost1\":\"minecraft:paper\",\"resultCount\":1,\"result\":\"minecraft:book\"}");
        check(a12.equals(b12), "S12 disambiguated canonical key order equal");
        for (int i = 0; i < 10; i++) {
            check(Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, a12, 1)
                            .equals(Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, b12, 1)),
                    "S12 disambiguated same id run " + (i + 1));
        }

        // 种子敏感性汇总（同一输入 10 次 hashHex 必须完全一致）
        for (int i = 0; i < 10; i++) {
            check(Seeds.hashOf(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK).equals(hash),
                    "S9 hashHex stable run " + (i + 1));
        }
    }

    static void writeCsv(String out) {
        StringBuilder sb = new StringBuilder();
        sb.append("scenario,run,profId,level,occurrenceIndex,hashHex,defaultUuid,disambiguatedUuid,consistent\n");

        String hash = Seeds.hashOf(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
        UUID defBase = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
        for (int i = 0; i < 10; i++) {
            UUID u = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK);
            sb.append("S1_default_determinism,").append(i + 1).append(',').append(PROF_LIBRARIAN).append(',').append(LEVEL_3)
                    .append(",-1,").append(hash).append(',').append(u).append(",,").append(u.equals(defBase)).append('\n');
        }
        UUID disBase = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, 3);
        for (int i = 0; i < 10; i++) {
            UUID u = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, 3);
            sb.append("S6_disambiguated_determinism,").append(i + 1).append(',').append(PROF_LIBRARIAN).append(',').append(LEVEL_3)
                    .append(",3,").append(hash).append(",,").append(u).append(',').append(u.equals(disBase)).append('\n');
        }
        for (int idx = 0; idx < 5; idx++) {
            UUID u = Seeds.disambiguatedId(PROF_LIBRARIAN, LEVEL_3, CANON_PAPER_BOOK, idx);
            sb.append("S7_occurrence_spread,").append(idx + 1).append(',').append(PROF_LIBRARIAN).append(',').append(LEVEL_3)
                    .append(',').append(idx).append(',').append(hash).append(",,").append(u).append(",true\n");
        }
        String ca = Canon.canonicalize("{\"b\":1,\"a\":2}");
        String cb = Canon.canonicalize("{\"a\":2,\"b\":1}");
        for (int i = 0; i < 10; i++) {
            UUID ua = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, ca);
            UUID ub = Seeds.defaultId(PROF_LIBRARIAN, LEVEL_3, cb);
            sb.append("S5_canonical_key_order,").append(i + 1).append(',').append(PROF_LIBRARIAN).append(',').append(LEVEL_3)
                    .append(",-1,").append(Seeds.hashOf(PROF_LIBRARIAN, LEVEL_3, ca)).append(',').append(ua).append(',').append(ub)
                    .append(',').append(ua.equals(ub)).append('\n');
        }

        try {
            Path p = Paths.get(out).toAbsolutePath();
            Files.createDirectories(p.getParent());
            Files.writeString(p, sb.toString(), StandardCharsets.UTF_8);
            System.out.println("[TradeIdStabilityTest] CSV written: " + p);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
