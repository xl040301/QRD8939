package com.android.systemui.keyguard;

public  abstract interface UpdateUnReadAndUncallListener {
	
	
    public abstract void updateUnCallNumber(int unCallNumber);
    
    
    public abstract void updateUnReadNumber(int unReadNumber);

}
