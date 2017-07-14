package com.ilusons.harmony.ref;

import android.content.Context;
import android.util.Log;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmEx {

    // Logger TAG
    private static final String TAG = RealmEx.class.getSimpleName();

    public static void init(Context context) {
        Log.d(TAG, "init::start");

        // Init realm
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(config);

        Log.d(TAG, "init::end");
    }

    public static Realm get(Context context) {
        Realm realm;
        try {
            realm = Realm.getDefaultInstance();
        } catch (Exception e) {
            init(context);
            return get(context);
        }
        return realm;
    }

}
