package com.chatter.chatter.service;

import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import com.chatter.chatter.dto.MediaStreamResult;
import com.chatter.chatter.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import javax.print.attribute.standard.Media;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    @Value("${azure.blob-storage.connection-string}")
    private String connectionString;

    @Value("${azure.blob-storage.account-name}")
    private String accountName;

    private BlobServiceClient blobServiceClient;

    private BlobServiceAsyncClient blobServiceAsyncClient;

    private final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @PostConstruct
    public void init() {
        blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        blobServiceAsyncClient = new BlobServiceClientBuilder()
                .retryOptions(new RequestRetryOptions(
                        RetryPolicyType.EXPONENTIAL,
                        5,
                        Duration.ofSeconds(300L),
                        null,
                        null,
                        null
                ))
                .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildAsyncClient();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() +
                (getFileExtension(file.getOriginalFilename()).isEmpty() ? "" :
                        "." + getFileExtension(file.getOriginalFilename()));

        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(fileName);

        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(
                        file.getContentType() != null ?
                                file.getContentType() :
                                "application/octet-stream"
                );

        try {
            if (file.getSize() > 10 * 1024 * 1024) {
                ParallelTransferOptions transferOpts = new ParallelTransferOptions()
                        .setBlockSizeLong(1024 * 1024L)
                        .setMaxConcurrency(2);

                BlobParallelUploadOptions uploadOptions = new BlobParallelUploadOptions(file.getInputStream())
                        .setParallelTransferOptions(transferOpts)
                        .setHeaders(headers);

                blobClient.uploadWithResponse(uploadOptions, null, null);
            } else {

                blobClient.upload(file.getInputStream(), file.getSize(), true);
                blobClient.setHttpHeaders(headers);
            }
        } catch (BlobStorageException e) {
            logger.error("Azure upload failed: {}", e.getMessage());
            throw new IOException("Failed to upload file to Azure", e);
        }

        return blobClient.getBlobName();
    }

    @Cacheable(value = "file", key = "'blobName:' + #blobName")
    public String getFileUrl(String blobName) {
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName);

        if (!blobClient.exists()) {
            throw new NotFoundException("message", "File not found");
        }

        UserDelegationKey delegationKey = blobServiceClient.getUserDelegationKey(
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1)
        );

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(3);

        BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission)
                .setStartTime(OffsetDateTime.now().minusMinutes(5));

        String sasToken = blobClient.generateUserDelegationSas(sasSignatureValues, delegationKey);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    public boolean isImage(MultipartFile file) {
        if (file == null || file.getContentType() == null) return false;
        return file.getContentType().startsWith("image");
    }

    public boolean isVideo(MultipartFile file) {
        if (file == null || file.getContentType() == null) return false;
        return file.getContentType().startsWith("video");
    }

    public boolean isAudio(MultipartFile file) {
        if (file == null || file.getContentType() == null) return false;
        return file.getContentType().startsWith("audio");
    }

    public boolean isSizeValid(MultipartFile file, Long size) {
        return file.getSize() <= size;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

}
