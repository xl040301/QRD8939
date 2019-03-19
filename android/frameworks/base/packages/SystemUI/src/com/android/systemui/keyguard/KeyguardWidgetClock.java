/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Bug 41927 20141226 wangdabin create */

package com.android.systemui.keyguard;

import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;
import android.util.Log;
import com.android.systemui.R;

/*
 * This display widget clock with two hands for hours, minutes and seconds.
 */

public class KeyguardWidgetClock extends View {
    private static final String TAG = "KeyguardWidgetClock";

    // redraw the clock widget
    private final static int REDRAW = 1;
    private Time mTime;

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    private static final int[] mWidgetClock = {
        0x01010102, 0x01010103, 0x01010104, 0x01010105
    };

    private static final int WidgetClockDial = 0;
    private static final int WidgetClockHandHour = 1;
    private static final int WidgetClockHandMinute = 2;
    private static final int WidgetClockHandSecond = 3;


    private int mDialWidth;
    private int mDialHeight;

    private final Handler mHandler = new Handler();

    private float mSecond;
    private float mMinutes;
    private float mHour;
    private boolean mAttached;
    private boolean mChanged;
    private float mClockwiseDivisor;


    public static final int ADJUST = 8;
    public KeyguardWidgetClock(Context context) {
        this(context, null);
    }

    public KeyguardWidgetClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardWidgetClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, mWidgetClock, defStyle, 0);

        mDial = a.getDrawable(WidgetClockDial);
        if (mDial == null) {
            mDial = r.getDrawable(R.drawable.clock_dial);
        }

        mHourHand = a.getDrawable(WidgetClockHandHour);
        if (mHourHand == null) {
            mHourHand = r.getDrawable(R.drawable.clock_hand_hour);
        }

        mMinuteHand = a.getDrawable(WidgetClockHandMinute);
        if (mMinuteHand == null) {
            mMinuteHand = r.getDrawable(R.drawable.clock_hand_minute);
        }

        mSecondHand = a.getDrawable(WidgetClockHandSecond);
        if (mSecondHand == null) {
            mSecondHand = r.getDrawable(R.drawable.clock_hand_second);
        }
        mClockwiseDivisor = Float.parseFloat(r.getString(R.string.keyguard_clockwise_divisor));

        mTime = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();

        Log.d(TAG, "KeyguardWidgetClock ");
    }

    private Handler mHandlerRedraw = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REDRAW:
                    invalidate();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendDelayedMessage() {
        Message message = new Message();
        message.what = REDRAW;
        mHandlerRedraw.sendMessageDelayed(message, 50);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, null);
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mTime = new Time();

        // Make sure we update to the current time
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandlerRedraw.removeCallbacksAndMessages(null);
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        sendDelayedMessage();
        onTimeChanged();
        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        int availableWidth = mRight - mLeft;
        int availableHeight = mBottom - mTop;

    
        int x = availableWidth / 2;
        int y = (int)(availableHeight / mClockwiseDivisor);

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                                   (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        //dial.draw(canvas); // background show, please open if need.

        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mSecond / 60.0f * 360.0f, x, y);
        final Drawable secondHand = mSecondHand;
        if (changed) {
            w = secondHand.getIntrinsicWidth();
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        secondHand.draw(canvas);
        canvas.restore();

        if (scaled) {
            canvas.restore();
        }
    }

    private void onTimeChanged() {
        mTime.setToNow();

        int hour = mTime.hour;
        int minute = mTime.minute;
        int second = mTime.second;
        mSecond = second;
        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;
        sendDelayedMessage();
        updateContentDescription(mTime);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mTime = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();
            invalidate();
        }
    };

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext, time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }
}
