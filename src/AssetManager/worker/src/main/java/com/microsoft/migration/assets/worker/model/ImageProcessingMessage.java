package com.microsoft.migration.assets.worker.model;

public class ImageProcessingMessage {
    private String key;
    private String contentType;
    private String storageType; // "azure-blob" or "local"
    private long size;

    public ImageProcessingMessage() {}

    public ImageProcessingMessage(String key, String contentType, String storageType, long size) {
        this.key = key;
        this.contentType = contentType;
        this.storageType = storageType;
        this.size = size;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}