package com.tienda.ropa.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ReportUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private static final DateTimeFormatter DISPLAY_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String generateReportFileName(String reportType, String format) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("reporte_%s_%s.%s", reportType, timestamp, format.toLowerCase());
    }

    public static String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(DISPLAY_FORMATTER);
    }

    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    public static Map<String, String> getContentTypeHeaders(String format) {
        Map<String, String> headers = new HashMap<>();
        
        switch (format.toLowerCase()) {
            case "pdf":
                headers.put("Content-Type", "application/pdf");
                break;
            case "xlsx":
            case "excel":
                headers.put("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "csv":
                headers.put("Content-Type", "text/csv");
                break;
            default:
                headers.put("Content-Type", "application/octet-stream");
        }
        
        return headers;
    }

    public static String sanitizeReportName(String name) {
        // Remover caracteres especiales para nombres de archivo
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static boolean isValidReportFormat(String format) {
        return format != null && (
                format.equalsIgnoreCase("pdf") || 
                format.equalsIgnoreCase("xlsx") || 
                format.equalsIgnoreCase("excel") ||
                format.equalsIgnoreCase("csv")
        );
    }

    public static String getReportDescription(String reportType) {
        switch (reportType.toLowerCase()) {
            case "products":
                return "Reporte completo de productos del catálogo";
            case "categories":
                return "Reporte de categorías de productos";
            case "users":
                return "Reporte de usuarios del sistema";
            case "inventory":
                return "Reporte de inventario y stock";
            case "sales":
                return "Reporte de ventas y transacciones";
            default:
                return "Reporte del sistema";
        }
    }
}