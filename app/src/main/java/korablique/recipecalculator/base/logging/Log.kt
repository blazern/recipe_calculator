package korablique.recipecalculator.base.logging

import android.content.Context
import androidx.annotation.NonNull
import com.bosphere.filelogger.FL
import com.bosphere.filelogger.FLConfig
import com.bosphere.filelogger.FLConst
import com.crashlytics.android.Crashlytics
import korablique.recipecalculator.BuildConfig
import org.jetbrains.annotations.NonNls
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalStateException

object Log {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
        if (!BuildConfig.DEBUG) {
            Timber.plant(CrashlyticsTimberTree())
        }
        Timber.plant(FileSystemTimberTree(context, logsDir()))
    }

    fun logsDir(): File = context.getDir("fllogs", Context.MODE_PRIVATE)

    /**
     * **DO NOT** report expected and external errors here (like a bad server response) - in
     * release builds, reported by this method exceptions go to Crashlytics.
     * @see Timber
     * */
    fun e(@NonNls message: String?, vararg args: Any?) = Timber.e(message, *args)
    /**
     * **DO NOT** report expected and external errors here (like a bad server response) - in
     * release builds, reported by this method exceptions go to Crashlytics.
     * @see Timber
     * */
    fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.e(t, message, *args)
    /**
     * **DO NOT** report expected and external errors here (like a bad server response) - in
     * release builds, reported by this method exceptions go to Crashlytics.
     * @see Timber
     * */
    fun e(t: Throwable?) = Timber.e(t)
    /** @see Timber */
    fun v(@NonNls message: String?, vararg args: Any?) = Timber.v(message, *args)
    /** @see Timber */
    fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.v(t, message, *args)
    /** @see Timber */
    fun v(t: Throwable?) = Timber.v(t)
    /** @see Timber */
    fun d(@NonNls message: String?, vararg args: Any?) = Timber.d(message, *args)
    /** @see Timber */
    fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.d(t, message, *args)
    /** @see Timber */
    fun d(t: Throwable?) = Timber.d(t)
    /** @see Timber */
    fun i(@NonNls message: String?, vararg args: Any?) = Timber.i(message, *args)
    /** @see Timber */
    fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.i(t, message, *args)
    /** @see Timber */
    fun i(t: Throwable?) = Timber.i(t)
    /** @see Timber */
    fun w(@NonNls message: String?, vararg args: Any?) = Timber.w(message, *args)
    /** @see Timber */
    fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.w(t, message, *args)
    /** @see Timber */
    fun w(t: Throwable?) = Timber.w(t)
    /** @see Timber */
    fun wtf(@NonNls message: String?, vararg args: Any?) = Timber.wtf(message, *args)
    /** @see Timber */
    fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.wtf(t, message, *args)
    /** @see Timber */
    fun wtf(t: Throwable?) = Timber.wtf(t)
}

private class CrashlyticsTimberTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, @NonNull message: String, t: Throwable?) {
        Crashlytics.log(priority, tag ?: "", message)
        if (t != null) {
            Crashlytics.log("Exception: $t")
        }
        if (priority == android.util.Log.ERROR && t != null) {
            Crashlytics.logException(t)
        }
    }
}

private class FileSystemTimberTree(
        context: Context,
        dir: File) : Timber.Tree() {
    init {
        FL.init(FLConfig.Builder(context)
                .logger(null)
                .logToFile(true)
                .dir(dir)
                .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                .maxFileCount(FLConst.DEFAULT_MAX_FILE_COUNT)
                .maxTotalSize(FLConst.DEFAULT_MAX_TOTAL_SIZE)
                .build())
        FL.setEnabled(true)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            android.util.Log.WARN -> logImpl(message, t) { msg -> FL.w(tag, msg) }
            android.util.Log.INFO -> logImpl(message, t) { msg -> FL.i(tag, msg) }
            android.util.Log.DEBUG -> logImpl(message, t) { msg -> FL.d(tag, msg) }
            android.util.Log.ASSERT -> logImpl(message, t) { msg -> FL.e(tag, msg) }
            android.util.Log.ERROR -> logImpl(message, t) { msg -> FL.e(tag, msg) }
            android.util.Log.VERBOSE -> logImpl(message, t) { msg -> FL.v(tag, msg) }
        }
    }

    private fun logImpl(message: String, t: Throwable?,
                        callable: (msg: String)->Unit) {
        callable.invoke(message)
        if (t != null) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            callable.invoke("Exception: $sw")
        }
    }
}
