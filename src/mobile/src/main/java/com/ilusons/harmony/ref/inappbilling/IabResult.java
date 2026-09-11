package com.ilusons.harmony.ref.inappbilling;

public class IabResult {
	public IabResult(int response, String message) {
	}

	public boolean isFailure() {
		return true;
	}

	public boolean isSuccess() {
		return false;
	}

	@Override
	public String toString() {
		return "IAB disabled";
	}
}
