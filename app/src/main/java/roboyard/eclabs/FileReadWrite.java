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
            System.out.println(e.getMessage());
            System.out.println("Exception readAssets");
            return null;

        }
        return aBuffer.toString();
    }

    /**
     * Writes the entire string to the specified file in append mode
     * The data created with this method is private to the application
     * Therefore, it is not accessible by the user and is deleted if the application is uninstalled
     * @param activity
     * @param fileLocation
     * @param content
     */
    public static void appendPrivateData(Activity activity, String fileLocation, String content) {
        FileOutputStream fOut = null;
        try {
            fOut = activity.openFileOutput(fileLocation, Context.MODE_APPEND);
            fOut.write(content.getBytes());
        } catch (Exception e) {
            System.out.println("Exception in appendPrivateData: " + e.getMessage());
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (Exception e) {
                    System.out.println("Error closing stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * overwrites the content of a file. Uses PRIVATE mode to overwrite instead of append
     * @param activity
     * @param fileLocation
     * @param content
     */
    public static void writePrivateData(Activity activity, String fileLocation, String content) {
        FileOutputStream fOut = null;
        try {
            fOut = activity.openFileOutput(fileLocation, Context.MODE_PRIVATE);
            fOut.write(content.getBytes());
        } catch (Exception e) {
            System.out.println("Exception in writePrivateData: " + e.getMessage());
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (Exception e) {
                    System.out.println("Error closing stream: " + e.getMessage());
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
            System.out.println("Exception in clearPrivateData: " + e.getMessage());
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (Exception e) {
                    System.out.println("Error closing stream: " + e.getMessage());
                }
            }
        }
    }

    public static String readPrivateData(Activity activity, String fileLocation) {
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
            System.out.println("Exception readPrivateData: " + e.toString());
            return "";
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception e) {
                    System.out.println("Error closing stream: " + e.getMessage());
                }
            }
        }
        // System.out.println("Map loaded: " + buffer.toString());
        return buffer.toString();
    }
}