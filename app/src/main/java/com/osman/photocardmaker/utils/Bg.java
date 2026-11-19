package com.osman.photocardmaker.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bg {
    private static final ExecutorService EX = Executors.newFixedThreadPool(3);
    private static final Handler H = new Handler(Looper.getMainLooper());

    public static void run(Runnable r) { EX.execute(r); }
    public static void main(Runnable r) { H.post(r); }
}
