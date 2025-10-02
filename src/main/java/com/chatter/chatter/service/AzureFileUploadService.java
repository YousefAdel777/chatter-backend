package com.chatter.chatter.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile("!test")
public class AzureFileUploadService implements FileUploadService {

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;
    private final FileValidationService fileValidationService;

//    @Value("${spring.cloud.azure.storage.blob.container-name}")
//    private String containerName;
//
//    @Value("${azure.blob-storage.connection-string}")
//    private String connectionString;
//
//    @Value("${azure.blob-storage.account-name}")
//    private String accountName;
//
//    private BlobServiceClient blobServiceClient;
//
//    private BlobServiceAsyncClient blobServiceAsyncClient;
//
//    @PostConstruct
//    public void init() {
//        blobServiceClient = new BlobServiceClientBuilder()
//                .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
//                .credential(new DefaultAzureCredentialBuilder().build())
//                .buildClient();
//
//        blobServiceAsyncClient = new BlobServiceClientBuilder()
//                .retryOptions(new RequestRetryOptions(
//                        RetryPolicyType.EXPONENTIAL,
//                        5,
//                        Duration.ofSeconds(300L),
//                        null,
//                        null,
//                        null
//                ))
//                .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
//                .credential(new DefaultAzureCredentialBuilder().build())
//                .buildAsyncClient();
//    }

    @Override
    public String uploadFile(MultipartFile file)  {
        String fileName = UUID.randomUUID() +
                (fileValidationService.getFileExtension(file.getOriginalFilename()).isEmpty() ? "" :
                        "." + fileValidationService.getFileExtension(file.getOriginalFilename()));

        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

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
        } catch (BlobStorageException | IOException e) {
            throw new RuntimeException("Failed to upload file to Azure: ", e);
        }

        return blobClient.getBlobName();
    }

    @Override
    @Cacheable(value = "file", key = "'blobName:' + #blobName")
    public String getFileUrl(String blobName) {
        return "http://localhost:8080/" + blobName;
//        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
////        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName);
//
//        if (!blobClient.exists()) {
//            throw new NotFoundException("message", "File not found");
//        }
//
//        UserDelegationKey delegationKey = blobServiceClient.getUserDelegationKey(
//                OffsetDateTime.now(),
//                OffsetDateTime.now().plusDays(1)
//        );
//
//        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(3);
//
//        BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
//        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission)
//                .setStartTime(OffsetDateTime.now().minusMinutes(5));
//
//        String sasToken = blobClient.generateUserDelegationSas(sasSignatureValues, delegationKey);
//        return blobClient.getBlobUrl() + "?" + sasToken;
    }

}
