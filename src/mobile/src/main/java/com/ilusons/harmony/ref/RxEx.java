package com.ilusons.harmony.ref;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxEx {

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36";

	public static Observable<Integer> downloadFile(final String url, final File toFile) {
		return Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> oe) throws Exception {
				InputStream inputStream = null;
				FileOutputStream outputStream = null;
				try {
					OkHttpClient client = new OkHttpClient.Builder().build();

					Request request = new Request.Builder()
							.url(url)
							.header("User-Agent", USER_AGENT)
							.build();
					Response response = client.newCall(request).execute();

					inputStream = response.body().byteStream();
					outputStream = new FileOutputStream(toFile);
					int totalCount = inputStream.available();
					byte[] buffer = new byte[2 * 1024];
					int len;
					int readLen = 0;
					while ((len = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, len);
						readLen += len;
						oe.onNext(totalCount <= 0 ? -1 : (int) ((float) readLen * 100 / (float) totalCount));
					}
				} catch (Exception e) {
					e.printStackTrace();
					oe.onError(e);
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
						if (outputStream != null) {
							outputStream.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				oe.onComplete();
			}
		});
	}

}
