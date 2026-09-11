package com.ilusons.harmony.ref.inappbilling;

public class IabException extends Exception {
	public IabException(IabResult r) {
		super(String.valueOf(r));
	}

	public IabException(int response, String message) {
		super(message);
	}

	public IabResult getResult() {
		return new IabResult(0, "disabled");
	}
}
