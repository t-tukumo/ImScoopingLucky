package ttukumo.scoop

import android.util.Log

object Logs {
    private const val TAG = "ImScoopingLucky";

    fun e(m: String) {
        Log.e(TAG, m)
    }

    fun i(m: String) {
        Log.i(TAG, m)
    }

    fun d(m: String) {
        Log.d(TAG, m)
    }
}
