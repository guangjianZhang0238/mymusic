package com.music.common.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.util.StringUtils;

/**
 * 拼音工具类
 */
public class PinyinUtils {
    
    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();
    
    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }
    
    /**
     * 获取汉字的拼音首字母
     * @param chinese 汉字字符串
     * @return 拼音首字母大写
     */
    public static String getFirstLetter(String chinese) {
        if (!StringUtils.hasText(chinese)) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        char[] chars = chinese.toCharArray();
        
        for (char c : chars) {
            if (c >= 0x4e00 && c <= 0x9fa5) { // 中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        sb.append(pinyinArray[0].charAt(0));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 忽略转换异常
                }
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                // 英文字母直接添加
                sb.append(Character.toUpperCase(c));
            } else if (c >= '0' && c <= '9') {
                // 数字直接添加
                sb.append(c);
            }
            // 其他字符忽略
        }
        
        return sb.toString();
    }
    
    /**
     * 获取完整的拼音
     * @param chinese 汉字字符串
     * @return 完整拼音大写
     */
    public static String getPinyin(String chinese) {
        if (!StringUtils.hasText(chinese)) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        char[] chars = chinese.toCharArray();
        
        for (char c : chars) {
            if (c >= 0x4e00 && c <= 0x9fa5) { // 中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        sb.append(pinyinArray[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 忽略转换异常
                }
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                // 英文字母直接添加
                sb.append(Character.toUpperCase(c));
            } else if (c >= '0' && c <= '9') {
                // 数字直接添加
                sb.append(c);
            }
            // 其他字符忽略
        }
        
        return sb.toString();
    }
    
    /**
     * 判断字符串是否匹配拼音首字母
     * @param text 原始文本
     * @param firstLetter 拼音首字母
     * @return 是否匹配
     */
    public static boolean matchesFirstLetter(String text, String firstLetter) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(firstLetter)) {
            return false;
        }
        
        String textFirstLetter = getFirstLetter(text);
        return textFirstLetter.startsWith(firstLetter.toUpperCase());
    }
    
    /**
     * 判断字符串是否匹配完整拼音或拼音的一部分
     * @param text 原始文本
     * @param pinyin 拼音关键词
     * @return 是否匹配
     */
    public static boolean matchesPinyin(String text, String pinyin) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(pinyin)) {
            return false;
        }
        
        String textPinyin = getPinyin(text);
        String upperPinyin = pinyin.toUpperCase();
        
        // 完整拼音匹配
        if (textPinyin.equals(upperPinyin)) {
            return true;
        }
        
        // 拼音部分匹配
        if (textPinyin.contains(upperPinyin)) {
            return true;
        }
        
        // 首字母匹配
        String firstLetter = getFirstLetter(text);
        if (firstLetter.startsWith(upperPinyin)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 模糊匹配 - 支持拼音首字母、全拼、部分拼音等多种匹配方式
     * @param text 原始文本
     * @param keyword 搜索关键词
     * @return 匹配得分（越高越匹配）
     */
    public static int fuzzyMatchScore(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return 0;
        }
        
        String upperKeyword = keyword.toUpperCase();
        int score = 0;
        
        // 1. 完全匹配得最高分
        if (text.equalsIgnoreCase(keyword)) {
            return 100;
        }
        
        // 2. 拼音首字母匹配
        String firstLetter = getFirstLetter(text);
        if (firstLetter.startsWith(upperKeyword)) {
            score += 80;
            // 长度越接近得分越高
            if (firstLetter.length() == upperKeyword.length()) {
                score += 10;
            }
        } else if (firstLetter.contains(upperKeyword)) {
            score += 60;
        }
        
        // 3. 完整拼音匹配
        String pinyin = getPinyin(text);
        if (pinyin.equals(upperKeyword)) {
            score += 90;
        } else if (pinyin.startsWith(upperKeyword)) {
            score += 70;
            // 长度越接近得分越高
            if (pinyin.length() == upperKeyword.length()) {
                score += 10;
            }
        } else if (pinyin.contains(upperKeyword)) {
            score += 50;
        }
        
        // 4. 原文匹配
        if (text.toUpperCase().startsWith(upperKeyword)) {
            score += 40;
        } else if (text.toUpperCase().contains(upperKeyword)) {
            score += 20;
        }
        
        return score;
    }
}