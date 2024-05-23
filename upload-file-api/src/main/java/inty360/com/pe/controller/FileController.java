package inty360.com.pe.controller;

import inty360.com.pe.configuration.CustomException;
import inty360.com.pe.dto.FileDto;
import inty360.com.pe.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/file")
public class FileController {

    Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam(value = "file") MultipartFile file,
                                     @RequestParam(value = "hash") String hash,
                                     @RequestParam(value = "index") Integer index,
                                     @RequestParam(value = "total") Integer total) {
        FileDto fileDto = new FileDto();
        fileDto.setFile(file);
        fileDto.setHash(hash);
        fileDto.setIndex(index);
        fileDto.setTotal(total);

        logger.info("Uploading chunk index {} for: {}", index, file.getOriginalFilename());

        try {
            this.fileService.saveFile(fileDto);
            if (index.equals(total)) {
                this.fileService.mergeFiles(fileDto);
                this.fileService.checkFile(fileDto);
            }
        } catch (CustomException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", e.getErrorCode());
            error.put("message", e.getMessage());

            logger.error("Error uploading file", e);
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Chunk index {} uploaded successfully", index);
        return new ResponseEntity<>("Chunk index " + index + " uploaded successfully!", HttpStatus.OK);
    }

}
