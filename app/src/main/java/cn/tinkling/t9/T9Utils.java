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

    //把字符型数字转化成字母
    static char convertDigitToInitial(char c) {
        //c='03'-->(char)(67+(51-35))=(char)83=S
        //由此可见，字符数字2-9(键盘上代表字母的几个数字按键)可转化为字母R-Y，用以标记
        return (char) ('C' + (c - '#'));
    }

    static boolean isInitial(char c) {
        return (c >= 'C') && (c <= 'Y');
    }

    /**
     * 将指定字符格式化为T9字符
     *
     * @param c 输入字符
     * @return T9字符
     */
    public static char formatCharToT9(char c) {
        if (c >= 'A' && c <= 'Z') {
            Log.e("formatCharToT9-AZ--", "char=" + PINYIN_T9_MAP[c - 'A']);
            return PINYIN_T9_MAP[c - 'A'];
        } else if (c >= 'a' && c <= 'z') {
            Log.e("formatCharToT9-az--", "char=" + PINYIN_T9_MAP[c - 'a']);
            return PINYIN_T9_MAP[c - 'a'];
        } else if (isValidT9Key(c)) {
            Log.e("formatCharToT9-valid--", "char=" + c);
            return c;
        }

        return '\0';
    }

    @NonNull
    private static String convertPinyinToT9Key(String py) {
        if (py == null || py.length() == 0) {
            Log.e("convertPinyinToT9Key---", "t9Key=null");
            return " ";
        }

        StringBuilder t9KeyBuilder = getReusableStringBuilder();

        for (int i = 0; i < py.length(); i++) {
            char c = py.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                char t9C = formatCharToT9(c); //c='D'-->t9C='3'-->
                if (i == 0) {
                    //convertDigitToInitial: return (char) ('C' + (c - '#'))-->67+(51-35)=83->S
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
        Log.e("convertPinyinToT9Key---", "t9Key=" + t9Key);
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
                Log.e("buildT9Key---", "t9Key=" + t9c);
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
        Log.e("buildT9Key--return-", "t9Key=" + t9Key);
        return t9Key;
    }

}
