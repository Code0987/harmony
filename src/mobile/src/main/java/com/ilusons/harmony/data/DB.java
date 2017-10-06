package com.ilusons.harmony.data;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class DB {

	// Logger TAG
	private static final String TAG = DB.class.getSimpleName();

	public static RealmConfiguration getDBConfig() {
		return new RealmConfiguration.Builder()
				.name("music.realm")
				.deleteRealmIfMigrationNeeded()
				.build();
	}

	public static Realm getDB() {
		try {
			return Realm.getInstance(getDBConfig());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
