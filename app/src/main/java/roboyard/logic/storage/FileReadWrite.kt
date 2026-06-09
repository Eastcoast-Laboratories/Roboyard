package roboyard.logic.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Created by Alain on 21/01/2015.
 */
class FileReadWrite {
    companion object {
        /**
         * overwrites the content of a file. Uses PRIVATE mode to overwrite instead of append
         * @param context
         * @param fileLocation
         * @param content
         * @return true if write was successful, false otherwise
         */
        @JvmStatic
        fun writePrivateData(context: Context?, fileLocation: String, content: String): Boolean {
            if (context == null) {
                Timber.d("Context is null in writePrivateData for file: %s", fileLocation)
                return false
            }

            var output: FileOutputStream? = null
            return try {
                output = context.openFileOutput(fileLocation, Context.MODE_PRIVATE)
                output.write(content.toByteArray(StandardCharsets.UTF_8))
                true
            } catch (e: Exception) {
                Timber.d("Exception in writePrivateData for file: $fileLocation: ${e.message}")
                false
            } finally {
                try {
                    output?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing stream: %s", e.message)
                }
            }
        }

        /**
         * Read private data from a file.
         * @param context The context.
         * @param fileLocation The file location.
         * @return The data.
         */
        @JvmStatic
        fun readPrivateData(context: Context?, fileLocation: String): String {
            if (context == null) {
                Timber.d("Context is null in readPrivateData for file: %s", fileLocation)
                return ""
            }

            val buffer = StringBuilder()
            var input: FileInputStream? = null
            var reader: BufferedReader? = null
            try {
                val file = context.getFileStreamPath(fileLocation)
                if (!file.exists()) {
                    return ""
                }

                input = context.openFileInput(fileLocation)
                reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))

                while (true) {
                    val line = reader.readLine() ?: break
                    buffer.append(line).append('\n')
                }

                if (buffer.isNotEmpty() && buffer[buffer.length - 1] == '\n') {
                    buffer.setLength(buffer.length - 1)
                }
            } catch (e: Exception) {
                Timber.d("Exception readPrivateData: %s", e.toString())
                return ""
            } finally {
                try {
                    reader?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing reader: %s", e.message)
                }
                try {
                    input?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing stream: %s", e.message)
                }
            }
            return buffer.toString()
        }

        /**
         * Check if a private data file exists
         * @param context
         * @param fileLocation
         * @return true if the file exists, false otherwise
         */
        @JvmStatic
        fun privateDataExists(context: Context, fileLocation: String): Boolean {
            return try {
                val file = context.getFileStreamPath(fileLocation)
                file.exists()
            } catch (e: Exception) {
                Timber.d("Exception in privateDataExists: ${e.message}")
                false
            }
        }

        /**
         * Delete a private data file
         * @param context
         * @param fileLocation
         * @return true if the file was deleted, false otherwise
         */
        @JvmStatic
        fun deletePrivateData(context: Context, fileLocation: String): Boolean {
            return try {
                val file = context.getFileStreamPath(fileLocation)
                if (file.exists()) file.delete() else false
            } catch (e: Exception) {
                Timber.d("Exception in deletePrivateData: ${e.message}")
                false
            }
        }

        /**
         * List all files in a directory in the app's private storage
         * @param context
         * @param dirName
         * @return array of file names, or null if the directory doesn't exist
         */
        @JvmStatic
        fun listPrivateDirectory(context: Context, dirName: String): Array<String>? {
            return try {
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) dir.list() else null
            } catch (e: Exception) {
                Timber.d("Exception in listPrivateDirectory: ${e.message}")
                null
            }
        }

        /**
         * Get the path to a save game file
         * @param context The context
         * @param slotId The slot ID for the save game
         * @return The absolute path to the save game file
         */
        @JvmStatic
        fun getSaveGamePath(context: Context, slotId: Int): String {
            val fileName = "save_$slotId.dat"
            val saveDir = File(context.filesDir, "saves")
            if (!saveDir.exists()) {
                saveDir.mkdirs()
            }
            return File(saveDir, fileName).absolutePath
        }

        /**
         * Load data from an absolute path
         * @param path The absolute path to the file
         * @return The file contents as a string, or null if the file could not be read
         */
        @JvmStatic
        fun loadAbsoluteData(path: String?): String? {
            if (path == null) {
                Timber.d("Path is null in loadAbsoluteData")
                return null
            }

            val content = StringBuilder()
            var input: FileInputStream? = null
            return try {
                val file = File(path)
                if (!file.exists()) {
                    Timber.d("File does not exist: %s", path)
                    return null
                }

                input = FileInputStream(file)
                BufferedReader(InputStreamReader(input)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        content.append(line).append('\n')
                    }
                }
                content.toString()
            } catch (e: Exception) {
                Timber.d("Exception in loadAbsoluteData for file: %s: %s", path, e.message)
                null
            } finally {
                try {
                    input?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing stream: %s", e.message)
                }
            }
        }

        /**
         * Write a bitmap to a file in private storage
         * @param context The context
         * @param fileLocation The file name (not full path)
         * @param bitmap The bitmap to save
         * @return true if write was successful, false otherwise
         */
        @JvmStatic
        fun writeBitmap(context: Context?, fileLocation: String, bitmap: Bitmap?): Boolean {
            if (context == null || bitmap == null) {
                Timber.d("Context or bitmap is null in writeBitmap for file: %s", fileLocation)
                return false
            }

            var output: FileOutputStream? = null
            return try {
                output = context.openFileOutput(fileLocation, Context.MODE_PRIVATE)
                bitmap.compress(CompressFormat.PNG, 100, output)
                true
            } catch (e: Exception) {
                Timber.d("Exception in writeBitmap for file: $fileLocation: ${e.message}")
                false
            } finally {
                try {
                    output?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing stream: %s", e.message)
                }
            }
        }

        /**
         * Read a bitmap from a file in private storage
         * @param context The context
         * @param fileLocation The file name (not full path)
         * @return The bitmap, or null if the file could not be read
         */
        @JvmStatic
        fun readBitmap(context: Context?, fileLocation: String): Bitmap? {
            if (context == null) {
                Timber.d("Context is null in readBitmap for file: %s", fileLocation)
                return null
            }

            var input: FileInputStream? = null
            return try {
                input = context.openFileInput(fileLocation)
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Timber.d("Exception in readBitmap for file: $fileLocation: ${e.message}")
                null
            } finally {
                try {
                    input?.close()
                } catch (e: Exception) {
                    Timber.d("Error closing stream: %s", e.message)
                }
            }
        }
    }
}
