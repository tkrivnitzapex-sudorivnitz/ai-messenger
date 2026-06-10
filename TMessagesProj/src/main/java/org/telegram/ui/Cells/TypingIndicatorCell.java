/*
 * Apex (Nekogram fork): inline "agent is typing" bubble.
 *
 * Rendered as the bottom-most row of the chat list (a real adapter item), so it
 * scrolls together with the messages — like ChatGPT/Claude — instead of living
 * in the action-bar subtitle.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TypingDotsDrawable;

public class TypingIndicatorCell extends View {

    private final Theme.ResourcesProvider resourcesProvider;
    private final TypingDotsDrawable dots;
    private final Paint bubblePaint;
    private final RectF bubbleRect = new RectF();

    private final int cellHeight = AndroidUtilities.dp(48);
    private final int bubbleHeight = AndroidUtilities.dp(34);
    private final int bubbleWidth = AndroidUtilities.dp(64);
    private final int sideMargin = AndroidUtilities.dp(12);
    private final int cornerRadius = AndroidUtilities.dp(16);

    public TypingIndicatorCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dots = new TypingDotsDrawable(true);
        dots.setIgnoreAnimationLocks();
        dots.setCallback(new Drawable.Callback() {
            @Override
            public void invalidateDrawable(Drawable who) {
                invalidate();
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {
            }

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {
            }
        });
    }

    private int color(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), cellHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        dots.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dots.stop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int top = (getMeasuredHeight() - bubbleHeight) / 2;
        final boolean rtl = LocaleController.isRTL;
        final int left = rtl ? getMeasuredWidth() - sideMargin - bubbleWidth : sideMargin;

        bubblePaint.setColor(color(Theme.key_chat_inBubble));
        bubbleRect.set(left, top, left + bubbleWidth, top + bubbleHeight);
        canvas.drawRoundRect(bubbleRect, cornerRadius, cornerRadius, bubblePaint);

        dots.setColor(color(Theme.key_chat_inTimeText));
        final int dotsLeft = left + (bubbleWidth - AndroidUtilities.dp(18)) / 2;
        final int dotsTop = top + AndroidUtilities.dp(8);
        dots.setBounds(dotsLeft, dotsTop, dotsLeft + AndroidUtilities.dp(18), dotsTop + AndroidUtilities.dp(18));
        dots.draw(canvas);
    }
}
