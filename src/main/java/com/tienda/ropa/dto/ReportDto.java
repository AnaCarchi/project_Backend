package com.tienda.ropa.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDto {
    private String reportType;
    private String format;
    private String fileName;
    private LocalDateTime generatedAt;
    private Long fileSize;
    private String downloadUrl;
    private String status;
    private Integer totalRecords;
}
