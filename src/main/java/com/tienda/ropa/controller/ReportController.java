package com.tienda.ropa.controller;

import com.tienda.ropa.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    // ENDPOINTS PARA MÓVILES (devuelven base64)
    @GetMapping("/products/pdf/mobile")
    public ResponseEntity<?> generateProductsPdfReportMobile() {
        try {
            log.info(" Generando reporte PDF de productos para móvil");
            byte[] pdfBytes = reportService.generateProductsPdfReport();
            
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            String fileName = "reporte_productos_" + getCurrentTimestamp() + ".pdf";
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("mimeType", "application/pdf");
            response.put("size", pdfBytes.length);
            response.put("base64Data", base64Pdf);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info(" Reporte PDF generado - Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(" Error generando reporte PDF de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando reporte PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/products/excel/mobile")
    public ResponseEntity<?> generateProductsExcelReportMobile() {
        try {
            log.info(" Generando reporte Excel de productos para móvil");
            byte[] excelBytes = reportService.generateProductsExcelReport();
            
            String base64Excel = Base64.getEncoder().encodeToString(excelBytes);
            String fileName = "reporte_productos_" + getCurrentTimestamp() + ".xlsx";
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.put("size", excelBytes.length);
            response.put("base64Data", base64Excel);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info(" Reporte Excel generado - Tamaño: {} bytes", excelBytes.length);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(" Error generando reporte Excel de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando reporte Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/categories/pdf/mobile")
    public ResponseEntity<?> generateCategoriesPdfReportMobile() {
        try {
            log.info(" Generando reporte PDF de categorías para móvil");
            byte[] pdfBytes = reportService.generateCategoriesPdfReport();
            
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            String fileName = "reporte_categorias_" + getCurrentTimestamp() + ".pdf";
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("mimeType", "application/pdf");
            response.put("size", pdfBytes.length);
            response.put("base64Data", base64Pdf);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info(" Reporte PDF de categorías generado - Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(" Error generando reporte PDF de categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando reporte PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/users/excel/mobile")
    public ResponseEntity<?> generateUsersExcelReportMobile() {
        try {
            log.info(" Generando reporte Excel de usuarios para móvil");
            byte[] excelBytes = reportService.generateUsersExcelReport();
            
            String base64Excel = Base64.getEncoder().encodeToString(excelBytes);
            String fileName = "reporte_usuarios_" + getCurrentTimestamp() + ".xlsx";
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.put("size", excelBytes.length);
            response.put("base64Data", base64Excel);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info(" Reporte Excel de usuarios generado - Tamaño: {} bytes", excelBytes.length);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(" Error generando reporte Excel de usuarios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando reporte Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/inventory/pdf/mobile")
    public ResponseEntity<?> generateInventoryReportMobile() {
        try {
            log.info(" Generando reporte PDF de inventario para móvil");
            byte[] pdfBytes = reportService.generateInventoryReport();
            
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            String fileName = "reporte_inventario_" + getCurrentTimestamp() + ".pdf";
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("mimeType", "application/pdf");
            response.put("size", pdfBytes.length);
            response.put("base64Data", base64Pdf);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info(" Reporte PDF de inventario generado - Tamaño: {} bytes", pdfBytes.length);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(" Error generando reporte PDF de inventario: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generando reporte PDF: " + e.getMessage()));
        }
    }

    //  ENDPOINTS ORIGINALES PARA WEB (devuelven archivo directamente)
    @GetMapping("/products/pdf")
    public ResponseEntity<byte[]> generateProductsPdfReport() {
        try {
            byte[] pdfBytes = reportService.generateProductsPdfReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "reporte_productos_" + getCurrentTimestamp() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generando reporte PDF de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/products/excel")
    public ResponseEntity<byte[]> generateProductsExcelReport() {
        try {
            byte[] excelBytes = reportService.generateProductsExcelReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", 
                "reporte_productos_" + getCurrentTimestamp() + ".xlsx");
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generando reporte Excel de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/categories/pdf")
    public ResponseEntity<byte[]> generateCategoriesPdfReport() {
        try {
            byte[] pdfBytes = reportService.generateCategoriesPdfReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "reporte_categorias_" + getCurrentTimestamp() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generando reporte PDF de categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/excel")
    public ResponseEntity<byte[]> generateUsersExcelReport() {
        try {
            byte[] excelBytes = reportService.generateUsersExcelReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", 
                "reporte_usuarios_" + getCurrentTimestamp() + ".xlsx");
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generando reporte Excel de usuarios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/inventory/pdf")
    public ResponseEntity<byte[]> generateInventoryReport() {
        try {
            byte[] pdfBytes = reportService.generateInventoryReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "reporte_inventario_" + getCurrentTimestamp() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generando reporte PDF de inventario: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableReports() {
        try {
            Map<String, Object> reports = new HashMap<>();
            
            // Reportes para web
            Map<String, String> productReports = new HashMap<>();
            productReports.put("pdf", "/api/reports/products/pdf");
            productReports.put("excel", "/api/reports/products/excel");
            
            Map<String, String> categoryReports = new HashMap<>();
            categoryReports.put("pdf", "/api/reports/categories/pdf");
            
            Map<String, String> userReports = new HashMap<>();
            userReports.put("excel", "/api/reports/users/excel");
            
            Map<String, String> inventoryReports = new HashMap<>();
            inventoryReports.put("pdf", "/api/reports/inventory/pdf");
            
            // Reportes para móvil
            Map<String, String> productReportsMobile = new HashMap<>();
            productReportsMobile.put("pdf", "/api/reports/products/pdf/mobile");
            productReportsMobile.put("excel", "/api/reports/products/excel/mobile");
            
            Map<String, String> categoryReportsMobile = new HashMap<>();
            categoryReportsMobile.put("pdf", "/api/reports/categories/pdf/mobile");
            
            Map<String, String> userReportsMobile = new HashMap<>();
            userReportsMobile.put("excel", "/api/reports/users/excel/mobile");
            
            Map<String, String> inventoryReportsMobile = new HashMap<>();
            inventoryReportsMobile.put("pdf", "/api/reports/inventory/pdf/mobile");
            
            reports.put("web", Map.of(
                "products", productReports,
                "categories", categoryReports,
                "users", userReports,
                "inventory", inventoryReports
            ));
            
            reports.put("mobile", Map.of(
                "products", productReportsMobile,
                "categories", categoryReportsMobile,
                "users", userReportsMobile,
                "inventory", inventoryReportsMobile
            ));
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error obteniendo reportes disponibles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }
}