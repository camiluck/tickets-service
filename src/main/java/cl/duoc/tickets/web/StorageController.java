package cl.duoc.tickets.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.tickets.dto.EfsFileInfo;
import cl.duoc.tickets.dto.S3ObjectInfo;
import cl.duoc.tickets.service.EfsStorageService;
import cl.duoc.tickets.service.S3StorageService;

/** Endpoints de apoyo para mostrar en la demo que hay en EFS y en S3. */
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final EfsStorageService efs;
    private final S3StorageService s3;

    public StorageController(EfsStorageService efs, S3StorageService s3) {
        this.efs = efs;
        this.s3 = s3;
    }

    /** Archivos temporales en el EFS montado. */
    @GetMapping("/efs")
    public Map<String, Object> efsFiles() {
        List<EfsFileInfo> files = efs.listFiles();
        return Map.of("root", efs.root(), "total", files.size(), "files", files);
    }

    /** Objetos en el bucket S3 (prefijo opcional: evento123/ o evento123/usuarioX/). */
    @GetMapping("/s3")
    public Map<String, Object> s3Objects(@RequestParam(required = false) String prefix) {
        List<S3ObjectInfo> objects = s3.list(prefix);
        return Map.of("bucket", String.valueOf(s3.bucketName()), "prefix", prefix == null ? "" : prefix,
                "total", objects.size(), "objects", objects);
    }
}
