package com.hatim.makmanager

import android.app.Application

// The error happened because ": Application()" was missing
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // This is where global initialization happens
    }
}