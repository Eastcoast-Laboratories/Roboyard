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

    public static String readAssets(Activity activity, String fileLocation)
    {
        StringBuilder aBuffer = new StringBuilder();

        try {
            Resources resources;

            resources = activity.getResources();
            InputStream iS = resources.getAssets().open(fileLocation);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(iS));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                aBuffer.append(aDataRow).append("\n");
            }

            myReader.close();

        } catch (Exception e) {
            Timber.d(e.getMessage());
            Timber.d("Exception readAssets");
            return null;

        }
        return aBuffer.toString();
    }

    /**
     * Append private data to a file.
     * @param activity The activity.
     * @param fileLocation The file location.
     * @param content The data.
     * @return True if successful, false otherwise.
     */
    public static boolean appendPrivateData(Activity activity, String fileLocation, String content) {
        if (activity == null) {
            Timber.d("Activity is null in appendPrivateData for file: %s", fileLocation);
            return false;
        }
        FileOutputStream fOut = null;
        try {
            fOut = activity.openFileOutput(fileLocation, Context.MODE_APPEND);
            fOut.write(content.getBytes());
            return true;
        } catch (Exception e) {
            Timber.d("Exception in appendPrivateData for file: " + fileLocation + ": " + e.getMessage());
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

    public static void clearPrivateData(Activity activity, String fileLocation) {
        FileOutputStream fOut = null;
        try {
            // Use PRIVATE mode to truncate file
            fOut = activity.openFileOutput(fileLocation, Context.MODE_PRIVATE);
            fOut.write(new byte[0]);
        } catch (Exception e) {
            Timber.d("Exception in clearPrivateData: " + e.getMessage());
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
     * Check if a private data file exists
     * @param activity The activity context
     * @param filename The name of the file to check
     * @return True if the file exists, false otherwise
     */
    public static boolean privateDataFileExists(Activity activity, String filename) {
        if (activity == null) {
            Timber.d("Activity is null in privateDataFileExists");
            return false;
        }
        
        try {
            File file = new File(activity.getApplicationContext().getFilesDir(), filename);
            return file.exists();
        } catch (Exception e) {
            Timber.d("Exception in privateDataFileExists: %s", e.getMessage());
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
     * Create a directory in the app's private storage
     * @param activity
     * @param dirName
     * @return true if the directory was created, false otherwise
     */
    public static boolean createPrivateDirectory(Activity activity, String dirName) {
        try {
            File dir = new File(activity.getFilesDir(), dirName);
            if (!dir.exists()) {
                return dir.mkdir();
            }
            return true;
        } catch (Exception e) {
            Timber.d("Exception in createPrivateDirectory: " + e.getMessage());
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

    /**
     * Write string data to a file in a specified directory within app's private storage
     * @param activity The activity context
     * @param data The string data to write
     * @param directory The directory name within private storage
     * @param filename The filename to write to
     * @return True if successful, false otherwise
     */
    public static boolean writeStringToFile(Activity activity, String data, String directory, String filename) {
        if (activity == null || data == null || directory == null || filename == null) {
            Timber.e("Invalid parameters in writeStringToFile");
            return false;
        }
        
        try {
            // Create the directory if it doesn't exist
            File dir = new File(activity.getFilesDir(), directory);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Timber.e("Failed to create directory: %s", directory);
                    return false;
                }
            }
            
            // Create and write to the file
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.getBytes());
            fos.close();
            
            Timber.d("Successfully wrote data to %s/%s", directory, filename);
            return true;
        } catch (Exception e) {
            Timber.e("Error writing to file %s/%s: %s", directory, filename, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}