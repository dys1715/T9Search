package cn.tinkling.t9;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * T9工具类
 */
public final class T9Utils {

    public static final char T9_KEYS_DIVIDER = ';';

    private static final char[] VALID_T9_KEYS = {
            '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', '+', ',', '*', '#'
    };

    private static final char[] PINYIN_T9_MAP = {
            '2', '2', '2',
            '3', '3', '3',
            '4', '4', '4',
            '5', '5', '5',
            '6', '6', '6',
            '7', '7', '7', '7',
            '8', '8', '8',
            '9', '9', '9', '9'
    };

    private static final Pool<StringBuilder> STRING_BUILDER_POOL = new Pool<>(4);
    private static final Pool<BitSet> BIT_SET_POOL = new Pool<>(4);

    private T9Utils() {
    }

    @NonNull
    static StringBuilder getReusableStringBuilder() {
        StringBuilder sb = STRING_BUILDER_POOL.acquire();
        return (sb != null) ? sb : new StringBuilder();
    }

    static void recycleStringBuilder(@NonNull StringBuilder sb) {
        sb.setLength(0);
        STRING_BUILDER_POOL.release(sb);
    }

    @NonNull
    static BitSet getReusableBitSet() {
        BitSet bs = BIT_SET_POOL.acquire();
        return (bs != null) ? bs : new BitSet();
    }

    static void recycleBitSet(@NonNull BitSet bs) {
        bs.clear();
        BIT_SET_POOL.release(bs);
    }

    /**
     * 检测指定字符是否是有效的T9字符
     *
     * @param c 输入字符
     * @return <code>true</code> - 如果指定字符为有效的T9字符，<code>false</code> - 其他
     */
    public static boolean isValidT9Key(char c) {
        return ((c >= '0') && (c <= '9')) || (c == ',') || (c == '+') || (c == '*') || (c == '#');
    }

    /**
     * 检测输入是否是有效的T9键
     *
     * @param key 键
     * @return <code>true</code> - 如果输入为有效的T9键，<code>false</code> - 其他
     */
    public static boolean isValidT9Key(@NonNull CharSequence key) {
        final int LEN = key.length();
        for (int i = 0; i < LEN; i++) {
            if (!isValidT9Key(key.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static int getWordsCount(String key, int start, int end) {
        int count = 0;

        int len = key.length();
        if (end >= len) {
            end = len - 1;
        }

        char c;
        for (int i = start; i < end; i++) {
            c = key.charAt(i);
            if (i == start || c == ' ' || isInitial(c)) {
                count++;
            }
        }

        return count;
    }

    static char convertDigitToInitial(char c) {
        return (char) ('C' + (c - '#'));
    }

    static boolean isInitial(char c) {
        return (c >= 'C') && (c <= 'Y');
    }

    /**
     * 转换T9索引为对应的T9字符
     *
     * @param index T9索引。0~9 -> 0~9, '+' -> 10, ',' -> 11, '*' -> 12, '#' -> 13.
     * @return T9字符
     * @throws ArrayIndexOutOfBoundsException <code>index < 0 || index > 13</code>
     */
    public static char convertIndexToT9Key(int index) {
        return VALID_T9_KEYS[index];
    }

    /**
     * 转换T9字符为对应的T9索引
     *
     * @param c T9字符 - '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', ',', '*', '#'.
     * @return T9索引
     * @throws IllegalArgumentException
     */
    public static int convertT9CharToIndex(char c) {
        if ((c >= '0') && (c <= '9')) {
            return c - '0';
        }

        switch (c) {
            case '+':
                return 10;
            case ',':
                return 11;
            case '*':
                return 12;
            case '#':
                return 13;
            default:
                throw new IllegalArgumentException("INVALID T9 SEARCH CHARACTER");
        }
    }

    /**
     * 将指定字符格式化为T9字符
     *
     * @param c 输入字符
     * @return T9字符
     */
    public static char formatCharToT9(char c) {
        if (c >= 'A' && c <= 'Z') {
            Log.e("formatCharToT9-AZ--", "" + PINYIN_T9_MAP[c - 'A']);
            return PINYIN_T9_MAP[c - 'A'];
        } else if (c >= 'a' && c <= 'z') {
            Log.e("formatCharToT9-az--", "" + PINYIN_T9_MAP[c - 'a']);
            return PINYIN_T9_MAP[c - 'a'];
        } else if (isValidT9Key(c)) {
            Log.e("formatCharToT9-valid--", "" + c);
            return c;
        }

        return '\0';
    }

    static String convertToT9Key(String input) {
        StringBuilder sb = getReusableStringBuilder();
        char cLast = ' ';
        final int LEN = input.length();
        for (int i = 0; i < LEN; i++) {
            char cSrc = input.charAt(i);
            char t9c = formatCharToT9(cSrc);

            if (t9c == 0)
                t9c = ' ';
            else if (Character.isUpperCase(cSrc) || i == 0
                    || (Character.isLetter(cSrc) && !Character.isLetter(cLast)))
                t9c = convertDigitToInitial(t9c);
            else if (Character.isDigit(cSrc) && !Character.isDigit(cLast))
                t9c = convertDigitToInitial(t9c);
            else if (isValidT9Key(cSrc))
                t9c = convertDigitToInitial(t9c);

            sb.append(t9c);
            cLast = cSrc;
        }

        String result = sb.toString();
        recycleStringBuilder(sb);
        return result;
    }

    static String formatPinyin(String pinyin) {
        StringBuilder pinyinBuilder = getReusableStringBuilder();
        final int LEN = pinyin.length();
        char c;
        for (int i = 0; i < LEN; i++) {
            c = pinyin.charAt(i);
            if (i == 0) {
                c = Character.toUpperCase(c);
            } else {
                c = Character.toLowerCase(c);
            }
            pinyinBuilder.append(c);
        }

        String format = pinyinBuilder.toString();
        recycleStringBuilder(pinyinBuilder);
        return format;
    }

    static String formatNonPinyin(String src) {
        StringBuilder sb = getReusableStringBuilder();
        final int LEN = src.length();
        char c;
        for (int i = 0; i < LEN; i++) {
            c = src.charAt(i);
            if (Character.isLetter(c)) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }

        String format = sb.toString();
        recycleStringBuilder(sb);
        return format;
    }

    /**
     * Build T9 Key.
     *
     * @param pinyinTokens Pinyin tokens.
     * @return T9 Key.
     * @see PinyinToken
     * @deprecated use {@link #buildT9Key(String, PinyinProvider)}.
     */
    @NonNull
    @Deprecated
    public static String buildT9Key(@NonNull List<PinyinToken> pinyinTokens) {
        StringBuilder pinyinBuilder = getReusableStringBuilder();

        for (PinyinToken pinyinToken : pinyinTokens) {
            if (PinyinToken.PINYIN == pinyinToken.type) {
                pinyinBuilder.append(formatPinyin(pinyinToken.target));
            } else {
                pinyinBuilder.append(formatNonPinyin(pinyinToken.target));
            }
        }

        String t9Key = convertToT9Key(pinyinBuilder.toString());
        recycleStringBuilder(pinyinBuilder);
        return t9Key;
    }

    @NonNull
    private static String convertPinyinToT9Key(String py) {
        if (py == null || py.length() == 0)
            return " ";

        StringBuilder t9KeyBuilder = getReusableStringBuilder();

        for (int i = 0; i < py.length(); i++) {
            char c = py.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                char t9C = formatCharToT9(c);
                if (i == 0) {
                    t9C = convertDigitToInitial(t9C);
                }

                t9KeyBuilder.append(t9C);
            } else {
                t9KeyBuilder.setLength(0);
                t9KeyBuilder.append(' ');
                break;
            }
        }

        String t9Key = t9KeyBuilder.toString();
        recycleStringBuilder(t9KeyBuilder);
        return t9Key;
    }

    private static void insertT9Key(@NonNull StringBuilder t9KeyBuilder, @NonNull String t9Str) {
        if (t9Str.length() == 0)
            return;

        int index = -1;
        while ((index = t9KeyBuilder.indexOf(String.valueOf(T9_KEYS_DIVIDER), index + 1)) >= 0) {
            t9KeyBuilder.insert(index, t9Str);
            index += t9Str.length();
        }
    }

    /**
     * Build T9 Key.
     *
     * @param src      input.
     * @param provider pinyin provider.
     * @return T9 Key.
     * @throws NullPointerException if src or provider is null.
     */
    @NonNull
    public static String buildT9Key(@NonNull String src, @NonNull PinyinProvider provider) {
        StringBuilder t9KeyBuilder = getReusableStringBuilder();
        t9KeyBuilder.append(T9_KEYS_DIVIDER);

        final int len = src.length();
        Log.e("buildT9Key---", "src=" + src + ", len=" + len);
        for (int i = 0; i < len; ++i) {
            char c = src.charAt(i);

            if (/*ASCII*/c < 128 ||/*Extended Latin*/(c < 0x250 || (0x1e00 <= c && c < 0x1eff))) {
                char t9c = convertDigitToInitial(formatCharToT9(c));
                insertT9Key(t9KeyBuilder, String.valueOf(t9c));
            } else {
                String[] pinyin = provider.getPinyin(c);
                Log.e("buildT9Key---", "pinyins=" + Arrays.toString(pinyin));
                if (pinyin == null || pinyin.length == 0) {
                    insertT9Key(t9KeyBuilder, " ");
                } else if (pinyin.length == 1) {
                    insertT9Key(t9KeyBuilder, convertPinyinToT9Key(pinyin[0]));
                } else {
                    String temp = t9KeyBuilder.toString();
                    StringBuilder tempBuilder = getReusableStringBuilder();

                    t9KeyBuilder.setLength(0);
                    for (String py : pinyin) {
                        tempBuilder.setLength(0);
                        tempBuilder.append(temp);
                        insertT9Key(tempBuilder, convertPinyinToT9Key(py));
                        t9KeyBuilder.append(tempBuilder);
                    }
                    recycleStringBuilder(tempBuilder);
                }
            }
        }

        t9KeyBuilder.delete(t9KeyBuilder.length() - 1, t9KeyBuilder.length());
        String t9Key = t9KeyBuilder.toString();
        recycleStringBuilder(t9KeyBuilder);
        return t9Key;
    }

}
