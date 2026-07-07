package com.scansetu.scansetu_app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.scansetu/native"

    external fun helloFromCpp(): String

    companion object {
        init {
            System.loadLibrary("scansetu_native")
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "helloFromCpp") {
                result.success(helloFromCpp())
            } else {
                result.notImplemented()
            }
        }
    }
}