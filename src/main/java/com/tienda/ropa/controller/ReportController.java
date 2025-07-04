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
            
            Map<String, String> productReports = new HashMap<>();
            productReports.put("pdf", "/api/reports/products/pdf");
            productReports.put("excel", "/api/reports/products/excel");
            
            Map<String, String> categoryReports = new HashMap<>();
            categoryReports.put("pdf", "/api/reports/categories/pdf");
            
            Map<String, String> userReports = new HashMap<>();
            userReports.put("excel", "/api/reports/users/excel");
            
            Map<String, String> inventoryReports = new HashMap<>();
            inventoryReports.put("pdf", "/api/reports/inventory/pdf");
            
            reports.put("products", productReports);
            reports.put("categories", categoryReports);
            reports.put("users", userReports);
            reports.put("inventory", inventoryReports);
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error obteniendo reportes disponibles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}