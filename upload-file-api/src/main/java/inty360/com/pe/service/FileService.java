package inty360.com.pe.service;

import inty360.com.pe.dto.FileDto;

public interface FileService {
    void saveFile(FileDto fileDto);
    void mergeFiles(FileDto fileDto);
    void checkFile(FileDto fileDto);
}
