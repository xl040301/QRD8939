package com.android.systemui.keyguard;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import android.util.Log;

public class MmsUnReadObserverExt extends ContentObserver {

	private static final String TAG = "MmsUnReadObserver";
	private Context mContext;
	private Handler mHandler;
	private UpdateUnReadAndUncallListener mListener;

	public MmsUnReadObserverExt(Handler handler) {
		super(handler);
		// TODO Auto-generated constructor stub
	}

	public MmsUnReadObserverExt(Handler handler, Context context) {
		super(handler);
		mContext = context;
		mHandler = handler;
	}

	public void setListener(
			UpdateUnReadAndUncallListener updateunreadanduncalllistener) {
		mListener = updateunreadanduncalllistener;
	}

	public Integer getUnreadMessageNum(Context context) {
		Cursor csr = null;
		int smsCount = 0;
		int mmsCount = 0;
		try {
			csr = context.getContentResolver().query(
					Uri.parse("content://sms"), null, "type = 1 and read = 0",
					null, null);
			smsCount = csr.getCount(); // miss msg count
		} catch (Exception e) {
			smsCount = 0;
		} finally {
			if (csr != null) {
				csr.close();
			}
		}

		try {
			csr = context.getContentResolver().query(
					Uri.parse("content://mms/inbox"), null,
					"read=0 and msg_box=1 and (m_type=132 or m_type=130)",
					null, null);
			mmsCount = csr.getCount();// miss mms count
		} catch (Exception e) {
			mmsCount = 0;
		} finally {
			if (csr != null) {
				csr.close();
				csr = null;
			}
		}

		return Integer.valueOf(smsCount + mmsCount);
	}
	
	public void refreshUnReadNumber() {

//		new AsyncTask() {
//
//			@Override
//			protected Integer doInBackground(Object... params) {
//				// TODO Auto-generated method stub
//				try {
//					return getUnreadMessageNum(mContext);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return 0;
//			}
//
//			protected void onPostExecute(Integer unReadMms) {
//				if (unReadMms.intValue() > 0) {
//					mListener.updateUnReadNumber(unReadMms.intValue());
//				}
//			}	
//		}.execute(new Void[]{null, null, null});
		
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Integer count = 0;
				try {
					count = getUnreadMessageNum(mContext);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//	if (count > 0) {
					mListener.updateUnReadNumber(count.intValue());
			//	}
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
