package com.android.systemui.keyguard;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.BatteryManager;
import android.os.RemoteException;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import android.text.TextUtils;
import android.widget.LinearLayout;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import android.view.WindowManagerGlobal;

import com.android.systemui.R;

public class KeyguardWidgetManager {
    private static final String TAG = "KeyguardWidgetManager";
    private static boolean DEBUG = false;

    private View mHostView = null;
    private Context mContext = null;

    private View mDataWidgetHostView = null;
    private TextView  mDataWidgetTextView = null;
    private TextView  mWeekWidgetTextView = null;

    private View mCallButtonHostView = null;
    private View mSmsButtonHostView  = null;
    private OnClickListener mMissedCallButtonListener = null;
    private OnClickListener mUnreadSmsButtonListener = null;
    private View mCallWidgetHostView = null;
    private View mSmsWidgetHostView  = null;
    private OnClickListener mMissedCallWidgetListener = null;
    private OnClickListener mUnreadSmsWidgetListener = null;

    private Calendar mCalendar = null;
    private SimpleDateFormat mFormater = null;
    private String mDateFormat = null;
    private String mTimeFormat = null;
    private String mDateSmallFormat = null;
    private String mTimeSmallFormat = null;
    private String mTimeFormatEvent = null;
    private ClockTicker mClockTicker = new ClockTicker();

    private static final String SYSTEM = "/system/fonts/";
    private static final String SYSTEM_FONT_TIME_FOREGROUND = SYSTEM + "AndroidClock_Highlight.ttf";
    private static final String DEFAULT_SORT_ORDER = "date DESC";
    private static final ComponentName MMS_COMPONENTNAME = new
            ComponentName("com.android.mms", "com.android.mms.ui.ConversationList");
    private static final ComponentName DIALER_COMPONENTNAME = new
            ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity");
    //private static final Typeface sForegroundFont = null;

    private LinearLayout mBatteryStatusView = null;
    private TextView  mBatterryLevelView = null;
    KeyguardUpdateMonitor mUpdateMonitor;
    // are we showing battery information?
    boolean mShowingBatteryInfo = false;
    boolean mShowingBatterylow = false;
    // last known plugged in state
    boolean mCharging = false;
    // last known battery level
    int mBatteryLevel = 100;
    int mBatteryLowLevel = 20;
    // Shadowed text values
    protected boolean mBatteryCharged;
    protected boolean mBatteryIsLow;
    private static final int[] BATTERY_STATUS_ICON = {
        R.drawable.stat_widget_battery_0,
        R.drawable.stat_widget_battery_1,
        R.drawable.stat_widget_battery_2,
        R.drawable.stat_widget_battery_3,
        R.drawable.stat_widget_battery_4,
        R.drawable.stat_widget_battery_5,
        R.drawable.stat_widget_battery_6,
        R.drawable.stat_widget_battery_7,
        R.drawable.stat_widget_battery_8,
        R.drawable.stat_widget_battery_9,
        R.drawable.stat_widget_battery_10,
        R.drawable.stat_widget_battery_11,
        R.drawable.stat_widget_battery_12,
        R.drawable.stat_widget_battery_13,
        R.drawable.stat_widget_battery_14,
        R.drawable.stat_widget_battery_15,
        R.drawable.stat_widget_battery_16,
        R.drawable.stat_widget_battery_17,
        R.drawable.stat_widget_battery_18,
        R.drawable.stat_widget_battery_19
    };
    static final int NOT_SHOW_BATTERY_STATUS = 101; //don't show battery icon

    private static final float SIZE_BATTERY_STANDARD = 32;
    private static final float SIZE_TIME_STANDARD = 38;

    private boolean mRegistered = false;
    private static KeyguardWidgetManager mKeyguardWidgetManager;
    
    private TextView mOwnerInfo;
    private LockPatternUtils mLockPatternUtils;
    
    UnReadAndMissCallObserverHelper observerHelper = null;
    private static final int MSG_MESSAGE_UPDATE = 1;
    private static final int MSG_MISSED_CALL_INIT = 2;

    public static KeyguardWidgetManager getInstance() {
        if (mKeyguardWidgetManager == null) {
            mKeyguardWidgetManager = new KeyguardWidgetManager();
        }
        return mKeyguardWidgetManager;
    }

    private KeyguardWidgetManager() {
    }

    public void setKeyguardWidgetManagerView(Context context, View viewHost, View viewMain){

        mContext = context;
        mHostView = viewHost;

        // for call/sms widget or big/small time.
        setDateTimeFormater();
        mCalendar = Calendar.getInstance();
        mFormater = new SimpleDateFormat(mTimeFormat, Locale.getDefault());

        // widget clock view
        View clockHostView = viewHost.findViewById(R.id.layout_widget_clock);
        clockHostView.setVisibility(View.VISIBLE);
        KeyguardWidgetClock widgetClockHostView = (KeyguardWidgetClock)viewHost.findViewById(R.id.widget_clock);
        widgetClockHostView.setVisibility(View.VISIBLE);

        // call/sms button view
        mMissedCallButtonListener = new OnClickListener(){
            @Override
            public void onClick(View arg0) {
            if (DEBUG) Log.e(TAG, "KeyguardWidgetManager mMissedCallButtonListener ");
                try {
                     WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
                } catch (RemoteException e) {
                    if (DEBUG) Log.w(TAG, "can't dismiss keyguard on launch");
                }
                Intent it = null;
                it = new Intent();
                it.setComponent(DIALER_COMPONENTNAME);
                it.setAction("android.intent.action.CALL");
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(it);
            }
        };
        mUnreadSmsButtonListener = new OnClickListener(){
            @Override
            public void onClick(View v) {
            if (DEBUG) Log.e(TAG, "KeyguardWidgetManager mUnreadSmsButtonListener ");
                try {
                     WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
                } catch (RemoteException e) {
                    if (DEBUG) Log.w(TAG, "can't dismiss keyguard on launch");
                }
                Intent it = null;
                it = new Intent();
                it.setComponent(MMS_COMPONENTNAME);   
                it.setAction("android.intent.action.VIEW");
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(it);
            }
        };
        mCallButtonHostView = viewHost.findViewById(R.id.layout_miss_call_button);
        mCallButtonHostView.setOnClickListener(mMissedCallButtonListener);
        mCallButtonHostView.setVisibility(View.VISIBLE);
        mSmsButtonHostView = viewHost.findViewById(R.id.layout_unread_sms_button);
        mSmsButtonHostView.setOnClickListener(mUnreadSmsButtonListener);
        mSmsButtonHostView.setVisibility(View.VISIBLE);



        // call/sms widget view
        mMissedCallWidgetListener = new OnClickListener(){
            @Override
            public void onClick(View arg0) {
            if (DEBUG) Log.e(TAG, "KeyguardWidgetManager mMissedCallWidgetListener ");
                try {
                    WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
                } catch (RemoteException e) {
                    if (DEBUG) Log.w(TAG, "can't dismiss keyguard on launch");
                }
                Intent it = null;
                it = new Intent();
                it.setComponent(DIALER_COMPONENTNAME);
                it.setAction("android.intent.action.CALL");
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(it);
            }
        };
        mUnreadSmsWidgetListener = new OnClickListener(){
            @Override
            public void onClick(View v) {
            if (DEBUG) Log.e(TAG, "KeyguardWidgetManager mUnreadSmsWidgetListener ");
                Intent it = null;
                it = new Intent();
                it.setComponent(MMS_COMPONENTNAME);   
                it.setAction("android.intent.action.VIEW");
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(it);
            }
        };
        mCallWidgetHostView = viewHost.findViewById(R.id.keyguard_missed_call_widget);
        mCallWidgetHostView.setOnClickListener(mMissedCallWidgetListener);
        mSmsWidgetHostView = viewHost.findViewById(R.id.keyguard_unread_sms_widget);
        mSmsWidgetHostView.setOnClickListener(mUnreadSmsWidgetListener);
        mBatteryLowLevel = 20;//context.getResources().getInteger(
                       // com.android.internal.R.integer.config_lowBatteryCloseWarningLevel);
        // big/small time view
        View dataTimeWidgetHostView = viewHost.findViewById(R.id.layout_date_time_widget);
        dataTimeWidgetHostView.setVisibility(View.GONE);

        // date/week view
        View dataWidgetHostView = viewHost.findViewById(R.id.layout_date_stand);
        dataWidgetHostView.setVisibility(View.VISIBLE);
        mDataWidgetTextView = (TextView)viewHost.findViewById(R.id.keyguard_date_stand);
        mDataWidgetTextView.setVisibility(View.VISIBLE);
        mWeekWidgetTextView = (TextView)viewHost.findViewById(R.id.keyguard_week_stand);
        mWeekWidgetTextView.setVisibility(View.VISIBLE);

        // battery view
        mBatteryStatusView = (LinearLayout) viewHost.findViewById(R.id.battery_status_view);
        mBatteryStatusView.setVisibility(View.GONE);
        mBatterryLevelView = (TextView) viewHost.findViewById(R.id.battery_level_text);
        mBatterryLevelView.setVisibility(View.GONE);
        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        mUpdateMonitor.registerCallback(mBatteryInfoCallback);
        updateBatteryInfo();
        
        mLockPatternUtils = new LockPatternUtils(mContext);
        mOwnerInfo = (TextView) viewHost.findViewById(R.id.owner_info);
        updateOwnerInfo();


        // for big/small time or data/week change listener
        mClockTicker.setTickerListener(mTickerListener);
        mClockTicker.start();
        
        observerHelper = new UnReadAndMissCallObserverHelper(mContext);
        observerHelper.registerContentProviderForUnRead(mUpdateUnReadAndUncallListener);
        
        // for big/small time or call/sms widget
        updateClock();
        // for data/week
        StringDataStandard();
    }

 

    private KeyguardUpdateMonitorCallback mBatteryInfoCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
      
            mShowingBatteryInfo = status.isPluggedIn();
            mShowingBatterylow = status.isBatteryLow();
          
            mCharging = status.status == BatteryManager.BATTERY_STATUS_CHARGING
                     || status.status == BatteryManager.BATTERY_STATUS_FULL;
            mBatteryLevel = status.level;
            mBatteryCharged = status.isCharged();
            mBatteryIsLow = status.isBatteryLow();
            if (DEBUG) {
                Log.d(TAG, "onRefreshBatteryInfo mShowingBatteryInfo = " + mShowingBatteryInfo);
                Log.d(TAG, "onRefreshBatteryInfo mShowingBatterylow = " + mShowingBatterylow);
                Log.d(TAG, "onRefreshBatteryInfo mCharging = " + mCharging);
                Log.d(TAG, "onRefreshBatteryInfo mBatteryLevel = " + mBatteryLevel);
                Log.d(TAG, "onRefreshBatteryInfo mBatteryCharged = " + mBatteryCharged);
                Log.d(TAG, "onRefreshBatteryInfo mBatteryIsLow = " + mBatteryIsLow);
                Log.d(TAG, "onRefreshBatteryInfo status.isPluggedIn() = " + status.isPluggedIn());
                Log.d(TAG, "onRefreshBatteryInfo status.isBatteryLow() = " + status.isBatteryLow());
            }
            updateBatteryInfo();
        }
        public void onScreenTurnedOff(int why) {
            if (DEBUG) Log.d(TAG, "onRefreshBatteryInfo onScreenTurnedOff mBatteryLevel = " + mBatteryLevel);
        };
        public void onScreenTurnedOn() {
            if (DEBUG) Log.d(TAG, "onRefreshBatteryInfo onScreenTurnedOn mBatteryLevel = " + mBatteryLevel);
        };
        
        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                updateOwnerInfo();
            }
        }
        
        @Override
        public void onUserSwitchComplete(int userId) {
            updateOwnerInfo();
        }
    };

    private void setBatteryLevelString(String string) {
        String str = string + "%";
        mBatterryLevelView.setText(str);
   
        float size = mBatterryLevelView.getTextSize();
        if (size > SIZE_BATTERY_STANDARD) {
            mBatterryLevelView.setTextSize(12);
        }

        mBatterryLevelView.setVisibility(View.VISIBLE);
    }

    private int getBatteryIconHandle(int level) {
        int iconIndex = 0;
        if (level <= 5) iconIndex = 0;
        else if (level <= 10) iconIndex = 1;
        else if (level <= 15) iconIndex = 2;
        else if (level <= 20) iconIndex = 3;
        else if (level <= 25) iconIndex = 4;
        else if (level <= 30) iconIndex = 5;
        else if (level <= 35) iconIndex = 6;
        else if (level <= 40) iconIndex = 7;
        else if (level <= 45) iconIndex = 8;
        else if (level <= 50) iconIndex = 9;
        else if (level <= 55) iconIndex = 10;
        else if (level <= 60) iconIndex = 11;
        else if (level <= 65) iconIndex = 12;
        else if (level <= 70) iconIndex = 13;
        else if (level <= 75) iconIndex = 14;
        else if (level <= 80) iconIndex = 15;
        else if (level < 90) iconIndex = 16;
        else if (level >= 90 && level < 95) iconIndex = 17;
        else if (level >= 95 && level <= 99) iconIndex = 18;
        else if (level == 100) iconIndex = 19;
        return iconIndex;
    }

    private void DrawablesBatteryImage(int index) {
        mBatteryStatusView.setBackgroundResource(BATTERY_STATUS_ICON[index]);
        mBatteryStatusView.setVisibility(View.VISIBLE);
    }

 
    /* delete function: getBatteryIconIndex() */
    void updateBatteryInfo() {
        if (mShowingBatteryInfo) {
            DrawablesBatteryImage(getBatteryIconHandle(mBatteryLevel));
            setBatteryLevelString(String.valueOf(mBatteryLevel));
        } else {
            if (mBatteryLevel < mBatteryLowLevel) {
                mBatteryStatusView.setBackgroundResource(R.drawable.stat_widget_battery_low);
                mBatteryStatusView.setVisibility(View.VISIBLE);
                mBatterryLevelView.setVisibility(View.GONE);
            } else {
                mBatteryStatusView.setVisibility(View.GONE);
                mBatterryLevelView.setVisibility(View.GONE);
            }
        }
    }

    private void showView(View v, int visibility){
        if (null != v){
            v.setVisibility(visibility);
        }
    }

    private void setViewText(View v, String text){
        if (null != v && v instanceof TextView){
            ((TextView)v).setText(text);
        }
    }

    private void setViewText(View v, int resid){
        if (null != v && v instanceof TextView){
            ((TextView)v).setText(resid);
        }
    }

    private void layoutDataTimeView(boolean hasEvent){
        if (hasEvent){
            showView(findViewById(R.id.layout_date_time), View.GONE);
            showView(findViewById(R.id.layout_date_time_small), View.VISIBLE);
        }else{
            showView(findViewById(R.id.layout_date_time), View.VISIBLE);
            showView(findViewById(R.id.layout_date_time_small), View.GONE);
        }
    }

    private Context getContext(){
        return mContext;
    }

    private View findViewById(int id){
        return mHostView.findViewById(id);
    }
    
    private synchronized void updateCallPreviewCount(int missedCall) {
        layoutDataTimeView(missedCall > 0 ? true : false);
       updateCallorSmsButton(mCallButtonHostView, R.drawable.ic_miss_call_button, missedCall);
       updateCallorSmsWidget(mCallWidgetHostView, R.drawable.ic_miss_call_widget, missedCall);
    }
    
   private synchronized void updateSmsPreviewCount(int unreadMsgCount) {
         layoutDataTimeView(unreadMsgCount > 0 ? true : false);
        updateCallorSmsButton(mSmsButtonHostView, R.drawable.ic_unread_sms_button, unreadMsgCount);
        updateCallorSmsWidget(mSmsWidgetHostView, R.drawable.ic_unread_sms_widget,unreadMsgCount);
    }

    private Handler    mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
         switch (msg.what) {
			case MSG_MESSAGE_UPDATE: {
				updateSmsPreviewCount(msg.arg1);
				break;
			}
			case MSG_MISSED_CALL_INIT: {
				updateCallPreviewCount(msg.arg1);
				break;
			}
			default: {
				return;
			}
		}
      }
    };
    
    private ContentObserver mContentObserver = new ContentObserver(new Handler()){
        @Override
        public void onChange(boolean selfChange) {
          
            try {
                setDateTimeFormater();
                updateClock();
                StringDataStandard();
                mHandler.sendEmptyMessage(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
          
        }
    };

    private Resources getResources(){
        return mContext.getResources();
    }
    
    private ContentResolver getContentResolver(){
        return mContext.getContentResolver();
    }
    

    private void updateCallorSmsButton(View viewHost, int drawableId, int count){
        ImageView viewIcon = null;
        LinearLayout viewStatusNumberIcon = null;
        TextView viewStatusNumberTxt = null;
        String missNumberStr = getResources().getString(R.string.miss_number);

        if (drawableId == R.drawable.ic_miss_call_button) {
            viewIcon = (ImageView)viewHost.findViewById(R.id.call_button_icon);
            viewStatusNumberIcon = (LinearLayout)viewHost.findViewById(R.id.call_status_number_icon);
            viewStatusNumberTxt = (TextView)viewHost.findViewById(R.id.call_status_number_text);
        } else if (drawableId == R.drawable.ic_unread_sms_button) {
            viewIcon = (ImageView)viewHost.findViewById(R.id.sms_call_icon);
            viewStatusNumberIcon = (LinearLayout)viewHost.findViewById(R.id.sms_status_number_icon);
            viewStatusNumberTxt = (TextView)viewHost.findViewById(R.id.sms_status_number_text);
        }

        if (count > 0){
            
            //viewIcon.setVisibility(View.VISIBLE);
            if (viewStatusNumberIcon.getVisibility() == View.GONE) {
                viewStatusNumberIcon.setVisibility(View.VISIBLE);
            }
			if (count > 99) {
			  viewStatusNumberTxt.setText(R.string.miss_number_large);
		    } else {
			  viewStatusNumberTxt.setText(String.format(missNumberStr, count));
			}
            float size = viewStatusNumberTxt.getTextSize();
            if (size == 28) {
                viewStatusNumberTxt.setTextSize(10);
            } else if (size == 31) {
                viewStatusNumberTxt.setTextSize(8);
            }
            if (viewStatusNumberTxt.getVisibility() == View.GONE) {
                viewStatusNumberTxt.setVisibility(View.VISIBLE);
            }
        }else{
            viewStatusNumberIcon.setVisibility(View.GONE);
            viewStatusNumberTxt.setVisibility(View.GONE);
        }
    }

    private void updateCallorSmsWidget(View viewHost, int drawableId, int count){
        ImageView viewIcon = null;
        ImageView viewStatusNumberIcon = null;
       TextView viewStatusNumberTxt = null;
        String missNumberStr = getResources().getString(R.string.miss_number);

        viewIcon = (ImageView)viewHost.findViewById(R.id.icon);
        viewStatusNumberIcon = (ImageView)viewHost.findViewById(R.id.status_number_icon);
        viewStatusNumberTxt = (TextView)viewHost.findViewById(R.id.number_text);

        if (count > 0){
            viewIcon.setImageResource(drawableId);
            viewStatusNumberIcon.setImageResource(R.drawable.ic_notification_status_number);
            viewStatusNumberTxt.setText(String.format(missNumberStr, count));
        }else{
            viewHost.setVisibility(View.GONE);
        }
    }

    public void StringDataStandard() {
        String year, month, day, week, strDate;
        Calendar c = Calendar.getInstance();
        TimeZone timeZone = c.getTimeZone();
        c.setTimeZone(timeZone);
        year = String.valueOf(c.get(Calendar.YEAR));
        month = String.valueOf(c.get(Calendar.MONTH) + 1);
        day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        week = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        if("1".equals(week)){
            week = getResources().getString(R.string.keyguard_week_sun);
        }else if("2".equals(week)){
            week = getResources().getString(R.string.keyguard_week_mon);
        }else if("3".equals(week)){
             week =getResources().getString(R.string.keyguard_week_tue);
         }else if("4".equals(week)){
            week = getResources().getString(R.string.keyguard_week_wed);
        }else if("5".equals(week)){
            week = getResources().getString(R.string.keyguard_week_thr);
        }else if("6".equals(week)){
            week = getResources().getString(R.string.keyguard_week_fri);
        }else if("7".equals(week)){
            week = getResources().getString(R.string.keyguard_week_sat);
        }
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.DATE_FORMAT);
        if("MM-dd-yyyy".equalsIgnoreCase(value)) {
            strDate = month + "/" + day + "/" + year;
        } else if ("dd-MM-yyyy".equalsIgnoreCase(value)) {
            strDate = day + "/" + month + "/" + year;
        } else if ("yyyy-MM-dd".equalsIgnoreCase(value)) {
            strDate = year + "/" + month + "/" + day;
        } else {
            strDate = year + "/" + month + "/" + day;
        }
        mDataWidgetTextView.setText(strDate);
        mDataWidgetTextView.setMaxLines(1);
        mDataWidgetTextView.setVisibility(View.VISIBLE);
        mWeekWidgetTextView.setText(week);
        mWeekWidgetTextView.setMaxLines(1);
        float size = mDataWidgetTextView.getTextSize();
        if (size > SIZE_TIME_STANDARD) {
            mDataWidgetTextView.setTextSize(16);
            mWeekWidgetTextView.setTextSize(17);
        }
        mWeekWidgetTextView.setVisibility(View.VISIBLE);
    }

    private ClockTicker.TickerListener mTickerListener = new ClockTicker.TickerListener(){
        @Override
        public void onTick() {
            updateClock();
            StringDataStandard();
        }
    };

    static class ClockTicker{
        private TickerListener mTickerListener = null;
        private boolean mTickerStopped = false;
        private Runnable mTicker;
        private Handler mHandler;
        public ClockTicker(){
            mHandler = new Handler();
            mTicker  = new Runnable() {
                public void run() {
                    if (mTickerStopped){
                        return;
                    }

                    if (null != mTickerListener){
                        mTickerListener.onTick();
                    }
                    long now = SystemClock.uptimeMillis();
                    long next = now + (1000 - now % 1000);
                    mHandler.postAtTime(mTicker, next);
                }
            };
        }

        public interface TickerListener{
            public void onTick();
        }

        public void start(){
            mTickerStopped = false;
            mTicker.run();
        }

        public void stop(){
            mTickerStopped = true;
        }

        public void setTickerListener(TickerListener tickerListener) {
            this.mTickerListener = tickerListener;
        }
        public TickerListener getTickerListener() {
            return mTickerListener;
        }
    }

    private void updateClock(){
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        String str;
        View v;

        // time
        mFormater.applyPattern(mTimeFormat);
        str = mFormater.format(mCalendar.getTime());
        setViewText(findViewById(R.id.keyguard_time), str);
        // time + am/pm
        mFormater.applyPattern(mTimeSmallFormat);
        str = mFormater.format(mCalendar.getTime());
        setViewText(findViewById(R.id.keyguard_time_small), str);

        v = findViewById(R.id.keyguard_ampm);
        if (is24HourFormat()){
            setViewText(v, "");
        }else{
            if (mCalendar.get(Calendar.AM_PM) == Calendar.AM){
                setViewText(v, R.string.kg_am);
            }else{
                setViewText(v, R.string.kg_pm);
            }
        }
        // week + date for bigger
        mFormater.applyPattern(mDateFormat);
        str = mFormater.format(mCalendar.getTime());
        setViewText(findViewById(R.id.keyguard_date_big), str);

        // week + date for smaller
        mFormater.applyPattern(mDateSmallFormat);
        str = mFormater.format(mCalendar.getTime());
        setViewText(findViewById(R.id.keyguard_date_small), str);
    }

    private boolean is24HourFormat(){
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    private void setDateTimeFormater(){
        int idDate = 0;
        int idTime = 0;
        int idTimeEvent = 0;
        int idDateSmall = 0;
        int idTimeSmall = 0;
        if (is24HourFormat()) {
            idDate = R.string.keyguard_date_format;
            idTime = R.string.keyguard_time_format_24;
            idTimeEvent = R.string.keyguard_time_format_small_24;
            idTimeSmall = R.string.keyguard_time_format_small_24;
            idDateSmall = R.string.keyguard_date_format;
        } else {
            idDate = R.string.keyguard_date_format;
            idTime = R.string.keyguard_time_format;
            idTimeEvent = R.string.keyguard_time_format_small;
            idTimeSmall = R.string.keyguard_time_format_small;
            idDateSmall = R.string.keyguard_date_format;
        }

        if (idDate != 0) {
            mDateFormat = null;
            mDateFormat = getResources().getString(idDate);
            if (DEBUG) Log.d(TAG, "setDateTimeFormater mDateFormat = " + mDateFormat);
        }
        
        if (idTime != 0) {
            mTimeFormat = null;
            mTimeFormat = getResources().getString(idTime);
            if (DEBUG) Log.d(TAG, "setDateTimeFormater mTimeFormat = " + mTimeFormat);
        }
        
        if (idTimeEvent != 0) {
            mTimeFormatEvent = null;
            mTimeFormatEvent = getResources().getString(idTimeEvent);
        }
        
        if (idDateSmall != 0) {
            mDateSmallFormat = null;
            mDateSmallFormat = getResources().getString(idDateSmall);
        }

        if (idTimeSmall != 0) {
            mTimeSmallFormat = null;
            mTimeSmallFormat = getResources().getString(idTimeSmall);
        }
   } 
   
  private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    } 
    
   private String getOwnerInfo() {
        ContentResolver res = mContext.getContentResolver();
        String info = null;
        final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled();
        if (ownerInfoEnabled) {
            info = mLockPatternUtils.getOwnerInfo(mLockPatternUtils.getCurrentUser());
        }
        return info;
    }
    
   UpdateUnReadAndUncallListenerImpl mUpdateUnReadAndUncallListener = new UpdateUnReadAndUncallListenerImpl();
   class UpdateUnReadAndUncallListenerImpl implements UpdateUnReadAndUncallListener {

		@Override
		public void updateUnCallNumber(int unCallNumber) {
			// TODO Auto-generated method stub
		//	if (getIndicatorNotifLockscreen() == 1) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_MISSED_CALL_INIT, unCallNumber, 0));
		//	}
		}

		@Override
		public void updateUnReadNumber(int unReadNumber) {
			// TODO Auto-generated method stub
		//	if (getIndicatorNotifLockscreen() == 1) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_MESSAGE_UPDATE, unReadNumber, 0));
		//	}
		}
    }
    
}
