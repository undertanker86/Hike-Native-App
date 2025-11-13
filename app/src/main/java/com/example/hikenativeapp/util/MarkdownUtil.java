package com.example.hikenativeapp.util;

import android.content.Context;
import android.widget.TextView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;

/**
 * Utility class để render Markdown text trong TextView
 * Sử dụng thư viện Markwon để hỗ trợ các cú pháp Markdown phổ biến:
 * - **bold**, *italic*, ~~strikethrough~~
 * - Lists (bullet, numbered)
 * - Tables
 * - Task lists
 * - Code blocks
 */
public class MarkdownUtil {

    private static Markwon markwon;

    /**
     * Initialize Markwon instance with plugins
     * @param context Application context
     */
    private static void initialize(Context context) {
        if (markwon == null) {
            markwon = Markwon.builder(context)
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(TaskListPlugin.create(context))
                    .build();
        }
    }

    /**
     * Display Markdown text into TextView
     * @param textView TextView to display
     * @param markdown Markdown string to display
     */
    public static void setMarkdown(TextView textView, String markdown) {
        if (textView == null || markdown == null) {
            return;
        }

        initialize(textView.getContext());
        markwon.setMarkdown(textView, markdown);
    }



    /**
     * Parse Markdown text thành Spanned (cho preview)
     * @param context Context
     * @param markdown Chuỗi Markdown cần parse
     * @return Spanned text với formatting
     */
    public static CharSequence parseMarkdown(Context context, String markdown) {
        if (context == null || markdown == null) {
            return markdown;
        }

        initialize(context);
        return markwon.toMarkdown(markdown);
    }
}

