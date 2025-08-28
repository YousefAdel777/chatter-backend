package com.chatter.chatter.config;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.*;

@TestConfiguration
public class AzureBlobStorageTestConfig {
    @Bean
    @Primary
    public BlobServiceClient blobServiceClient() {
        return mock(BlobServiceClient.class);
    }

    @Bean
    @Primary
    public BlobContainerClient blobContainerClient() {
        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
        BlobClient mockBlob = mock(BlobClient.class);

        when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlob);
        when(mockBlob.exists()).thenReturn(true);
        when(mockBlob.getBlobUrl()).thenReturn("https://mock-storage/test.jpg");
        when(mockBlob.getBlobName()).thenReturn("test.jpg");
        when(mockBlob.generateUserDelegationSas(any(), any())).thenReturn("sasToken");

        return mockContainer;
    }
}