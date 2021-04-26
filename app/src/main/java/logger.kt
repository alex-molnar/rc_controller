import android.util.Log
import com.example.rccontroller.BuildConfig

inline fun Any.debug(message: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d(this::class.java.simpleName, message())
    }
}

inline fun Any.info(message: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.i(this::class.java.simpleName, message())
    }
}

inline fun Any.warn(message: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.w(this::class.java.simpleName, message())
    }
}
