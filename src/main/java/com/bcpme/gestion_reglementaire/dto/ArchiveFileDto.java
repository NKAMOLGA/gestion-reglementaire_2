package com.bcpme.gestion_reglementaire.dto;

public class ArchiveFileDto {

    private String name;
    private long size;

    public ArchiveFileDto(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}