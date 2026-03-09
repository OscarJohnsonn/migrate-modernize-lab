package com.microsoft.migration.assets.model;

import java.time.Instant;

public class S3StorageItem {
    private String key;
    private String name;
    private long size;
    private Instant lastModified;
    private Instant uploadedAt;
    private String url;

    public S3StorageItem() {}

    public S3StorageItem(String key, String name, long size, Instant lastModified, Instant uploadedAt, String url) {
        this.key = key;
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.uploadedAt = uploadedAt;
        this.url = url;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name;}
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}