package roboyard.eclabs;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import timber.log.Timber;

/**
 * Created by Alain on 21/01/2015.
 */
public class FileReadWrite {
    public FileReadWrite(){

    }

    /**
     * overwrites the content of a file. Uses PRIVATE mode to overwrite instead of append
     * @param activity
     * @param fileLocation
     * @param content
     * @return true if write was successful, false otherwise
     */
    public static boolean writePrivateData(Activity activity, String fileLocation, String content) {
        if (activity == null) {
            Timber.d("Activity is null in writePrivateData for file: %s", fileLocation);
            return false;
        }
        FileOutputStream fOut = null;
        try {
            fOut = activity.openFileOutput(fileLocation, Context.MODE_PRIVATE);
            fOut.write(content.getBytes());
            return true;
        } catch (Exception e) {
            Timber.d("Exception in writePrivateData for file: " + fileLocation + ": " + e.getMessage());
            return false;
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (Exception e) {
                    Timber.d("Error closing stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Read private data from a file.
     * @param activity The activity.
     * @param fileLocation The file location.
     * @return The data.
     */
    public static String readPrivateData(Activity activity, String fileLocation) {
        if (activity == null) {
            Timber.d("Activity is null in readPrivateData for file: %s", fileLocation);
            return "";
        }
        
        StringBuilder buffer = new StringBuilder();
        FileInputStream fin = null;
        try {
            File file = activity.getApplicationContext().getFileStreamPath(fileLocation);
            if(file == null || !file.exists()) {
                return "";
            }

            fin = activity.openFileInput(fileLocation);

            int c;
            while((c = fin.read()) != -1) {
                buffer.append((char)c);
            }
        } catch (Exception e) {
            Timber.d("Exception readPrivateData: %s", e.toString());
            return "";
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception e) {
                    Timber.d("Error closing stream: %s", e.getMessage());
                }
            }
        }
        // Timber.d("Map loaded: " + buffer.toString());
        return buffer.toString();
    }

    /**
     * Check if a private data file exists
     * @param activity
     * @param fileLocation
     * @return true if the file exists, false otherwise
     */
    public static boolean privateDataExists(Activity activity, String fileLocation) {
        try {
            File file = activity.getApplicationContext().getFileStreamPath(fileLocation);
            return file != null && file.exists();
        } catch (Exception e) {
            Timber.d("Exception in privateDataExists: " + e.getMessage());
            return false;
        }
    }


    /**
     * Delete a private data file
     * @param activity
     * @param fileLocation
     * @return true if the file was deleted, false otherwise
     */
    public static boolean deletePrivateData(Activity activity, String fileLocation) {
        try {
            File file = activity.getApplicationContext().getFileStreamPath(fileLocation);
            if (file != null && file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            Timber.d("Exception in deletePrivateData: " + e.getMessage());
            return false;
        }
    }


    /**
     * List all files in a directory in the app's private storage
     * @param activity
     * @param dirName
     * @return array of file names, or null if the directory doesn't exist
     */
    public static String[] listPrivateDirectory(Activity activity, String dirName) {
        try {
            File dir = new File(activity.getFilesDir(), dirName);
            if (dir.exists() && dir.isDirectory()) {
                return dir.list();
            }
            return null;
        } catch (Exception e) {
            Timber.d("Exception in listPrivateDirectory: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the path to a save game file
     * @param activity The activity context
     * @param slotId The slot ID for the save game
     * @return The absolute path to the save game file
     */
    public static String getSaveGamePath(Activity activity, int slotId) {
        String fileName = "save_" + slotId + ".dat";
        File saveDir = new File(activity.getFilesDir(), "saves");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        return new File(saveDir, fileName).getAbsolutePath();
    }
    
    /**
     * Load data from an absolute path
     * @param path The absolute path to the file
     * @return The file contents as a string, or null if the file could not be read
     */
    public static String loadAbsoluteData(String path) {
        if (path == null) {
            Timber.d("Path is null in loadAbsoluteData");
            return null;
        }
        
        StringBuilder content = new StringBuilder();
        FileInputStream fIn = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                Timber.d("File does not exist: %s", path);
                return null;
            }
            
            fIn = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fIn));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            Timber.d("Exception in loadAbsoluteData for file: %s: %s", path, e.getMessage());
            return null;
        } finally {
            if (fIn != null) {
                try {
                    fIn.close();
                } catch (Exception e) {
                    Timber.d("Error closing stream: %s", e.getMessage());
                }
            }
        }
    }

}