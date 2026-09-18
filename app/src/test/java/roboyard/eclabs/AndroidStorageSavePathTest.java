package roboyard.eclabs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.junit.Assert.*;

import java.io.File;

import android.content.Context;
import roboyard.platform.AndroidStorage;

/**
 * Regression test: AndroidStorage must accept file names with path separators
 * ("saves/save_1.dat"). Context.getFileStreamPath() rejects them via
 * makeFilename(), which broke every save to the saves/ subdirectory.
 *
 * Run: ./gradlew testDebugUnitTest --tests "roboyard.eclabs.AndroidStorageSavePathTest"
 */
public class AndroidStorageSavePathTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AndroidStorage storage() {
        Context mockContext = Mockito.mock(Context.class);
        Mockito.when(mockContext.getFilesDir()).thenReturn(tempFolder.getRoot());
        return new AndroidStorage(mockContext);
    }

    @Test
    public void testWriteFileWithSubdirectoryPath() {
        AndroidStorage storage = storage();
        assertTrue("writeFile must succeed for saves/save_99.dat",
                storage.writeFile("saves/save_99.dat", "test-content"));
        assertTrue("File must exist under filesDir/saves/",
                new File(tempFolder.getRoot(), "saves/save_99.dat").exists());
    }

    @Test
    public void testReadFileExistsDeleteWithSubdirectoryPath() {
        AndroidStorage storage = storage();
        storage.writeFile("saves/save_98.dat", "hello");

        assertTrue(storage.fileExists("saves/save_98.dat"));
        assertEquals("hello\n", storage.readFile("saves/save_98.dat"));
        assertNotNull(storage.getFileTimestamp("saves/save_98.dat"));
        assertTrue(storage.getFilePath("saves/save_98.dat")
                .endsWith("saves" + File.separator + "save_98.dat"));
        assertTrue(storage.deleteFile("saves/save_98.dat"));
        assertFalse(storage.fileExists("saves/save_98.dat"));
    }

    @Test
    public void testFlatFileNameStillWorks() {
        AndroidStorage storage = storage();
        assertTrue(storage.writeFile("history_99.txt", "flat"));
        assertTrue(new File(tempFolder.getRoot(), "history_99.txt").exists());
        assertEquals("flat\n", storage.readFile("history_99.txt"));
    }
}
