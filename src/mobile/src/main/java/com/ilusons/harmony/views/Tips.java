package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ilusons.harmony.R;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class Tips {

	private static final String TAG = Tips.class.getSimpleName();

	private static final String DONT_SHOW_AGAIN = TAG + ".DONT_SHOW_AGAIN";

	private FragmentActivity activity;
	private SharedPreferences spref;

	public Tips(final FragmentActivity fragmentActivity) {
		activity = fragmentActivity;
		spref = activity.getSharedPreferences(TAG, 0);
	}

	public FragmentActivity getActivity() {
		return activity;
	}

	private Listener listener;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	private String title;

	public String getTitle() {
		if (title == null) {
			return "Tips";
		} else {
			return title;
		}
	}

	public void setTitle(String s) {
		title = s;
	}

	private ArrayList<String> messages = new ArrayList<>();

	public String getMessage() {
		if (messages.size() == 0)
			return null;
		return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
	}

	public void setMessages(ArrayList<String> messages) {
		this.messages.clear();
		this.messages.addAll(messages);
	}

	public String getMessages() {
		if (messages.size() == 0)
			return null;

		StringBuilder sb = new StringBuilder();
		for (String msg : messages)
			sb.append(msg).append(System.lineSeparator()).append(System.lineSeparator());

		return sb.toString();
	}

	private String positiveText;

	public String getPositiveText() {
		if (positiveText == null) {
			return "Got it!";
		} else {
			return positiveText;
		}
	}

	public void setPositiveText(String s) {
		positiveText = s;
	}

	private String negativeText;

	public String getNegativeText() {
		if (negativeText == null) {
			return "Not again";
		} else {
			return negativeText;
		}
	}

	public void setNegativeText(String s) {
		negativeText = s;
	}

	private String more;

	public String getMore() {
		if (more == null) {
			return "More ...";
		} else {
			return more;
		}
	}

	public void setMore(String s) {
		more = s;
	}

	public static void reset(Context context) {
		context.getSharedPreferences(TAG, 0).edit().clear().apply();

		Log.d(TAG, "Cleared shared preferences.");
	}

	private void showDialog() {
		if (activity == null || activity.isDestroyed() || activity.isFinishing())
			return;

		if (activity.getSupportFragmentManager().findFragmentByTag(TAG) != null) {
			return;
		}

		Fragment frag = new Fragment();
		frag.setOwner(this);
		frag.show(activity.getSupportFragmentManager(), TAG);
	}

	public void forceShow() {
		showDialog();
	}

	public void run() {
		try {
			if (spref.getBoolean(DONT_SHOW_AGAIN, false)) {
				return;
			}

			showDialog();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onNegative() {
		if (listener != null)
			try {
				listener.onNegative(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		try {
			Editor editor = spref.edit();
			editor.putBoolean(DONT_SHOW_AGAIN, true);
			editor.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onPositive() {
		if (listener != null)
			try {
				listener.onPositive(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public interface Listener {
		void onPositive(Tips tips);

		void onNegative(Tips tips);
	}

	public static class Fragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

		private Tips owner;

		public void setOwner(Tips owner) {
			this.owner = owner;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Fragment including variables will survive orientation changes
			this.setRetainInstance(true);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View v = inflater.inflate(R.layout.tips_dialog, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
			builder.setView(v);

			try {
				TextView title = v.findViewById(R.id.tips_dialog_title);
				title.setText(owner.getTitle());

				final TextView message = v.findViewById(R.id.tips_dialog_message);
				message.setText(owner.getMessage());

				TextView negative = v.findViewById(R.id.tips_dialog_negative);
				negative.setText(owner.getNegativeText());
				negative.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						owner.onNegative();

						dismiss();
					}
				});

				final TextView positive = v.findViewById(R.id.tips_dialog_positive);
				positive.setText(owner.getPositiveText());
				positive.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						owner.onPositive();

						dismiss();
					}
				});

				TextView more = v.findViewById(R.id.tips_dialog_more);
				more.setText(owner.getMore());
				more.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						message.setText(owner.getMessages());
					}
				});

				builder.setOnCancelListener(this);
			} catch (Exception e) {
				e.printStackTrace();
			}

			AlertDialog alert = builder.create();

			alert.requestWindowFeature(DialogFragment.STYLE_NO_TITLE);

			return alert;
		}

		@Override
		public void onCancel(DialogInterface dialog) {

		}

		@Override
		public void onClick(DialogInterface dialog, int choice) {
			switch (choice) {
				case DialogInterface.BUTTON_POSITIVE:
					owner.onPositive();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					owner.onNegative();
					break;
			}
		}

		@Override
		public void onDestroyView() {
			// Work around bug:
			// http://code.google.com/p/android/issues/detail?id=17423
			Dialog dialog = getDialog();

			if ((dialog != null) && getRetainInstance()) {
				dialog.setDismissMessage(null);
			}

			super.onDestroyView();
		}

	}

}
