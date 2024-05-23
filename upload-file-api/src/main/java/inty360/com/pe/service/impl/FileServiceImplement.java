package inty360.com.pe.service.impl;

import inty360.com.pe.configuration.CustomException;
import inty360.com.pe.dto.FileDto;
import inty360.com.pe.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

@Service
public class FileServiceImplement implements FileService {

    Logger logger = LoggerFactory.getLogger(FileServiceImplement.class);

    @Value("${file-upload-dir}")
    private String baseDir;

    @Override
    public void saveFile(FileDto fileDto) {
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = date.format(formatter);

        String uploadDir = this.baseDir + "/" + formattedDate + "/" + fileDto.getHash();
        String chunkFilename = uploadDir + "/" + fileDto.getIndex() + ".part";

        try {
            this.createDirectory(uploadDir);
            fileDto.getFile().transferTo(new File(chunkFilename));
        } catch (IOException e) {
            throw new CustomException("FILE_SAVE", "Could not save file chunk", e);
        }

        fileDto.setUploadDir(uploadDir);
    }

    @Override
    public void mergeFiles(FileDto fileDto) {
        try {
            Path mergedFilePath = Paths.get(fileDto.getUploadDir(), fileDto.getFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(mergedFilePath)) {
                IntStream.iterate(1, i -> i + 1)
                        .mapToObj(i -> Paths.get(fileDto.getUploadDir(), "/" + i + ".part"))
                        .takeWhile(Files::exists)
                        .forEach(chunkPath -> {
                            try {
                                Files.copy(chunkPath, out);
                                Files.delete(chunkPath);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new CustomException("FILE_MERGE", "Could not merge file chunks", e);
        }
    }

    @Override
    public void checkFile(FileDto fileDto) {
        Path path = Paths.get(fileDto.getUploadDir() + "/" + fileDto.getFile().getOriginalFilename());
        String hashFile = this.generateHashFile(path);

        if (!hashFile.equals(fileDto.getHash())) {
            logger.error("File hash does not match: {}", fileDto.getFile().getOriginalFilename());
            throw new CustomException("FILE_CORRUPTED", "File is corrupted, please upload again.");
        }

        logger.info("File hash matches: {}", fileDto.getFile().getOriginalFilename());
        logger.info("Client file hash: {}", fileDto.getHash());
        logger.info("Server file hash: {}", hashFile);
    }

    private void createDirectory(String pathDir) throws IOException {
        Path path = Paths.get(pathDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private String generateHashFile(Path filePath) {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = sha256Digest.digest(fileBytes);

            StringBuilder hash = new StringBuilder();
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }

            return hash.toString();
        } catch (Exception e) {
            throw new CustomException("FILE_HASH", "Could not generate hash file", e);
        }
    }

}
