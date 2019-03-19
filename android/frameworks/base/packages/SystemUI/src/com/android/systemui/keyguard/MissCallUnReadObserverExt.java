package com.android.systemui.keyguard;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;

import android.util.Log;

public class MissCallUnReadObserverExt extends ContentObserver {

    private static final String TAG = "MissCallUnReadObserver";
    private Context mContext;
    private Handler mHandler;
    UpdateUnReadAndUncallListener mListener;

	public MissCallUnReadObserverExt(Handler handler) {
		super(handler);
		// TODO Auto-generated constructor stub
	}
	
	public MissCallUnReadObserverExt(Handler handler,Context context) {
		super(handler);
		mContext = context;
		mHandler = handler;
	}
	
    public void setListener(UpdateUnReadAndUncallListener listener) {
        mListener = listener;
    }
	
	
	private Integer getMissedCallNum(Context context) throws Exception {
		Cursor csr = null;
		int missedCallCount = 0;
		try {
			csr = context.getContentResolver().query(Calls.CONTENT_URI,
					new String[] { Calls.NUMBER, Calls.TYPE, Calls.NEW }, null,
					null, Calls.DEFAULT_SORT_ORDER);
			if (null != csr) {
				while (csr.moveToNext()) {
					int type = csr.getInt(csr.getColumnIndex(Calls.TYPE));
					switch (type) {
					case Calls.MISSED_TYPE:
						if (csr.getInt(csr.getColumnIndex(Calls.NEW)) == 1) {
							missedCallCount++;
						}
						break;
					case Calls.INCOMING_TYPE:
						break;
					case Calls.OUTGOING_TYPE:
						break;
					}
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
			missedCallCount = 0;
		} finally {
		    if (csr != null) {
			   csr.close();
			}
		}
		return Integer.valueOf(missedCallCount);
	}
	
	
	public void refreshUnReadNumber() {
//		new AsyncTask() {
//
//			@Override
//			protected Integer doInBackground(Object... params) {
//				// TODO Auto-generated method stub
//				try {
//					return getMissedCallNum(mContext);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return 0;
//			}
//
//			protected void onPostExecute(Integer unReadCall) {
//				if (unReadCall.intValue() > 0) {
//					mListener.updateUnCallNumber(unReadCall.intValue());
//				}
//			}	
//		}.execute(new Void[]{null, null, null});
		
		
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Integer count = 0;
				try {
					count = getMissedCallNum(mContext);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//if (count > 0) {
					mListener.updateUnCallNumber(count.intValue());
				//}
			}
			
		}, 100);

	}

	@Override
	public void onChange(boolean selfChange) {
		// TODO Auto-generated method stub
		super.onChange(selfChange);
		refreshUnReadNumber();
	}
	

}
