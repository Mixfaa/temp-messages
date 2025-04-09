package com.mixfa.tempmessages.service;

import com.mixfa.tempmessages.model.FileData;
import com.mixfa.tempmessages.model.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    FileData write(MultipartFile file) throws Exception;

    FileData write(String filename, InputStream inputStream) throws Exception;

    FileResponse read(String id) throws Exception;

    void delete(String id)  throws Exception;
}
