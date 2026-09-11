package com.ilusons.harmony.ref.inappbilling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IabBroadcastReceiver extends BroadcastReceiver {
	public static final String ACTION = "com.android.vending.billing.PURCHASES_UPDATED";

	public interface IabBroadcastListener {
		void receivedBroadcast();
	}

	public IabBroadcastReceiver(IabBroadcastListener listener) {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	}
}
