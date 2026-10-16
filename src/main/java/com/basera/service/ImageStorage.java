package com.basera.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.basera.exception.AppException;
import com.basera.model.Image;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Saves an uploaded listing photo (config/upload.js).
 *
 *   - If CLOUD_NAME is set, the photo goes to Cloudinary.
 *   - Otherwise it's saved on disk in the `uploads` folder and served at /uploads/... (see config/WebConfig).
 *     Good enough for local demos - but Render's disk is wiped on every restart, so use Cloudinary there.
 *
 * The 5 MB limit is spring.servlet.multipart.max-file-size in application.properties (multer's `limits`).
 *
 * Cloudinary: the Node app used Cloudinary's SDK. Here we call Cloudinary's upload URL directly - it's one
 * HTTP request with a signature, and it saves us a large extra library.
 */
@Service
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    /** ALLOWED_TYPES, mapped to the file extension we save them with. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/webp", ".webp");

    private final Path uploadDir;
    private final String cloudName;
    private final String cloudApiKey;
    private final String cloudApiSecret;
    private final JsonMapper jsonMapper;
    private final SecureRandom random = new SecureRandom();

    public ImageStorage(@Value("${basera.upload-dir}") String uploadDir,
            @Value("${basera.cloudinary.cloud-name}") String cloudName,
            @Value("${basera.cloudinary.api-key}") String cloudApiKey,
            @Value("${basera.cloudinary.api-secret}") String cloudApiSecret,
            JsonMapper jsonMapper) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
        this.cloudName = cloudName;
        this.cloudApiKey = cloudApiKey;
        this.cloudApiSecret = cloudApiSecret;
        this.jsonMapper = jsonMapper;
        log.info(useCloudinary() ? "Image uploads go to Cloudinary ({})" : "Image uploads are saved in {}",
                useCloudinary() ? cloudName : this.uploadDir);
    }

    private boolean useCloudinary() {
        return cloudName != null && !cloudName.isBlank();
    }

    /** Saves the file and returns it as an Image, or null when no file was chosen (fileToImage). */
    public Image store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = ALLOWED_TYPES.get(String.valueOf(file.getContentType()).toLowerCase());
        if (extension == null) {
            // multer's fileFilter silently skipped other files; telling the user is friendlier.
            throw AppException.badRequest("Upload error: only PNG, JPG and WebP images are allowed.");
        }
        try {
            return useCloudinary() ? uploadToCloudinary(file) : saveToDisk(file, extension);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded image", e);
        }
    }

    /** multer.diskStorage: `${Date.now()}-${random}${ext}` in the uploads folder. */
    private Image saveToDisk(MultipartFile file, String extension) throws IOException {
        Files.createDirectories(uploadDir);
        String filename = System.currentTimeMillis() + "-" + random.nextInt(1_000_000_000) + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        return new Image("/uploads/" + filename, filename);
    }

    /**
     * Cloudinary's "signed upload": POST the file with our API key, a timestamp and a signature
     * (SHA-1 of the sorted parameters + the API secret), so only we can upload to our account.
     */
    private Image uploadToCloudinary(MultipartFile file) throws IOException {
        String folder = "basera";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = sha1Hex("folder=" + folder + "&timestamp=" + timestamp + cloudApiSecret);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { // the multipart part needs a file name
                return file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            }
        });
        form.add("api_key", cloudApiKey);
        form.add("timestamp", timestamp);
        form.add("folder", folder);
        form.add("signature", signature);

        String body = RestClient.create().post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload", cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(String.class);
        JsonNode result = jsonMapper.readTree(body);
        return new Image(result.get("secure_url").asString(), result.get("public_id").asString());
    }

    private static String sha1Hex(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
