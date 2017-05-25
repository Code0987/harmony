package com.ilusons.harmony

class Global {

    private var instance: Global? = null

    fun getInstance(): Global? {
        if (instance == null)
            instance = Global()
        return instance
    }

}
