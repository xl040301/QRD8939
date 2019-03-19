package com.android.systemui.keyguard;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.Telephony;
import android.util.Log;

public class UnReadAndMissCallObserverHelper {

	private Context mContext;
	private MmsUnReadObserverExt obMms = null;
	private MissCallUnReadObserverExt obMissCall = null;
	
	private static final Uri MMS_URI = Telephony.Threads.CONTENT_URI;
	private static final Uri MISS_CALL_URI = CallLog.Calls.CONTENT_URI;

	public UnReadAndMissCallObserverHelper(Context context) {
		mContext = context;
	}

	public void registerContentProviderForUnRead(UpdateUnReadAndUncallListener listener) {
        obMms = new MmsUnReadObserverExt(new Handler(),mContext);
        mContext.getContentResolver().registerContentObserver(MMS_URI, true, obMms);
        obMms.setListener(listener);
        obMms.refreshUnReadNumber();
        obMissCall = new MissCallUnReadObserverExt(new Handler(), mContext);
        mContext.getContentResolver().registerContentObserver(MISS_CALL_URI, true, obMissCall);
        obMissCall.setListener(listener);
        obMissCall.refreshUnReadNumber();
    }

	public void unRegisterContentProviderForUnRead() {
		if (obMms != null) {
			mContext.getContentResolver().unregisterContentObserver(obMms);
			obMms = null;
		}
		if (obMissCall != null) {
			mContext.getContentResolver().unregisterContentObserver(obMissCall);
			obMissCall = null;
		}
	}

}
