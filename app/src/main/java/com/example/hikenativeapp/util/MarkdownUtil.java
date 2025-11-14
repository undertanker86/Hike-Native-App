package com.example.hikenativeapp.util;

import android.content.Context;
import android.widget.TextView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;

/**
 * Utility class để render Markdown text trong TextView
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

}

