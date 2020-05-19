package korablique.recipecalculator.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import korablique.recipecalculator.R
import korablique.recipecalculator.base.logging.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val logsPreference: Preference = findPreference(getString(R.string.preference_key_send_logs))!!
        logsPreference.setOnPreferenceClickListener {
            val logsDir = Log.logsDir()

            // The "shared_logs" dir name is also used in file_paths.xml - don't change it.
            val sharedLogsDir = File(requireContext().cacheDir, "shared_logs")
            sharedLogsDir.mkdirs()
            val logsFile = File(sharedLogsDir, "logs.zip")

            val logsFiles = logsDir.listFiles()
            if (logsFiles.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_logs_yet, Toast.LENGTH_SHORT).show()
                return@setOnPreferenceClickListener true
            }

            try {
                zip(logsFiles, logsFile)
            } catch (e: IOException) {
                Log.e(e)
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                return@setOnPreferenceClickListener true
            }

            val logsUri = FileProvider.getUriForFile(
                    requireContext(), getString(R.string.file_provider_authority), logsFile)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, logsUri)
            shareIntent.type = "application/zip"
            startActivity(Intent.createChooser(shareIntent, getString(R.string.send_logs)))
            true
        }
    }
}

private fun zip(files: Array<File>, outFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { out ->
        val data = ByteArray(1024)
        for (file in files) {
            val inputStream = FileInputStream(file)
            BufferedInputStream(inputStream, 1024).use { origin ->
                val entry = ZipEntry(file.name)
                out.putNextEntry(entry)
                while (true) {
                    val read = origin.read(data, 0, 1024)
                    if (read == -1) {
                        break
                    }
                    out.write(data, 0, read)
                }
            }
        }
    }
}