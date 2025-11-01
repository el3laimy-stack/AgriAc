package accounting.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private final String dbFilePath;
    private final String backupDir;

    public BackupService(String dbFilePath, String backupDir) {
        this.dbFilePath = dbFilePath;
        this.backupDir = backupDir;
    }

    public String backupDatabase() throws IOException {
        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists()) {
            backupDirFile.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupFileName = "backup-" + timestamp + ".db";
        String backupFilePath = Paths.get(backupDir, backupFileName).toString();

        Files.copy(Paths.get(dbFilePath), Paths.get(backupFilePath), StandardCopyOption.REPLACE_EXISTING);
        
        return backupFilePath;
    }

    public void restoreDatabase(String backupFilePath) throws IOException {
        Files.copy(Paths.get(backupFilePath), Paths.get(dbFilePath), StandardCopyOption.REPLACE_EXISTING);
    }
}
