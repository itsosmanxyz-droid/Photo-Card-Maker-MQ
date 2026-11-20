package com.osman.photocardmaker.render;

import java.util.Calendar;

/** Formats today's date in Bangla, for example "১৭ নভেম্বর ২০২৬". */
public final class BanglaDate {

    private static final String[] MONTHS = {
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};

    private static final char[] DIGITS = {
            '০', '১', '২', '৩', '৪',
            '৫', '৬', '৭', '৮', '৯'};

    private BanglaDate() {
    }

    public static String toBanglaDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(DIGITS[ch - '0']);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String today() {
        Calendar c = Calendar.getInstance();
        String text = c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)]
                + " " + c.get(Calendar.YEAR);
        return toBanglaDigits(text);
    }
}
