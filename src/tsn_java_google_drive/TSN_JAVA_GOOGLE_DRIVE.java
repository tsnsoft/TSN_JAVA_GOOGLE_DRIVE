package tsn_java_google_drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.io.FileOutputStream;

public class TSN_JAVA_GOOGLE_DRIVE {

    // Имя приложения при запросе доступа (авторизации)
    private static final String APPLICATION_NAME = "TSN_JAVA_GOOGLE_DRIVE";
    // Локальная подпапка для работы с облаком (закачки в нее файлов)
    private static final String TOKENS_DIRECTORY_PATH = "google_drive_data";
    // Файл с настройками подключения (авторизации)
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // Метод подключения к учетной записи Google
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        Credential credential = new AuthorizationCodeInstalledApp(
                new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(
                                TSN_JAVA_GOOGLE_DRIVE.class.getResourceAsStream(CREDENTIALS_FILE_PATH))),
                        Collections.singletonList(DriveScopes.DRIVE))
                        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline").build(),
                new LocalServerReceiver.Builder().setPort(8888).build()).authorize("user");
        return credential;
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Папка для загрузки файлов с облака Google
        final String DIR = new java.io.File(".").getAbsoluteFile().getParentFile().getAbsolutePath()
                + System.getProperty("file.separator") + TOKENS_DIRECTORY_PATH + System.getProperty("file.separator");

        // Подключение к облаку
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME).build();
        
        
        // Запрос к облаку: найти все файлы не в корзине с именем влючающими "test" и расширением "txt"
        FileList result = service.files().list()
                .setQ("trashed = false and name contains 'test' and mimeType = 'text/plain'")
                .setFields("nextPageToken, files").execute();

        // Получение списка файлов из запроса
        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) { // Если файлов искомых нет
            System.out.println("No files found.");
        } else {
            // Если файлы искомые есть
            System.out.println("Files:");

            // Перебираем найденные файлы
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
                System.out.println(DIR + file.getName());
                // Загружаем локально файл с облака
                OutputStream outputStream = new FileOutputStream(DIR + file.getName());
                service.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
                outputStream.flush(); outputStream.close();
            }
        }
    }
}
