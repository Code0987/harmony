package com.ilusons.harmony.ref.inappbilling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/** No-op replacement for the retired AIDL IAB helper. */
public class IabHelper {
	public static class IabAsyncInProgressException extends Exception {
	}

	public interface OnIabSetupFinishedListener {
		void onIabSetupFinished(IabResult result);
	}

	public interface QueryInventoryFinishedListener {
		void onQueryInventoryFinished(IabResult result, Inventory inventory);
	}

	public interface OnIabPurchaseFinishedListener {
		void onIabPurchaseFinished(IabResult result, Purchase purchase);
	}

	public IabHelper(Context context, String key) {
	}

	public void enableDebugLogging(boolean enabled, String tag) {
	}

	public void startSetup(OnIabSetupFinishedListener listener) {
		if (listener != null) {
			listener.onIabSetupFinished(new IabResult(0, "disabled"));
		}
	}

	public void queryInventoryAsync(QueryInventoryFinishedListener listener) throws IabAsyncInProgressException {
		if (listener != null) {
			listener.onQueryInventoryFinished(new IabResult(0, "disabled"), new Inventory());
		}
	}

	public void launchPurchaseFlow(Activity activity, String sku, int request, OnIabPurchaseFinishedListener listener, String payload) throws IabAsyncInProgressException {
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return false;
	}

	public void dispose() {
	}

	public void disposeWhenFinished() {
	}
}
