package com.tienda.ropa.service;

import com.tienda.ropa.model.Category;
import com.tienda.ropa.model.Product;
import com.tienda.ropa.model.User;
// PDF
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public byte[] generateUsersExcelReport() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Usuarios");

        // Estilo para headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Headers - ARRAY CORREGIDO
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Usuario", "Email", "Rol", "Estado", "Bloqueado", "Fecha Registro"};
        
        // BUCLE FOR CORREGIDO - El problema estaba aquí
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        List<User> users = userService.getAllUsers();
        int rowNum = 1;
        
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getEmail());
            
            String role = user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .findFirst()
                    .orElse("ROLE_USER");
            row.createCell(3).setCellValue(role);
            
            row.createCell(4).setCellValue(user.getEnabled() ? "Habilitado" : "Deshabilitado");
            row.createCell(5).setCellValue(user.getLocked() ? "Bloqueado" : "Desbloqueado");
            row.createCell(6).setCellValue(user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        log.info("Reporte Excel de usuarios generado con {} registros", users.size());
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generateProductsExcelReport() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Productos");

        // Estilo para headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Nombre", "Descripción", "Categoría", "Precio", "Stock", "Estado", "Fecha Creación"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        List<Product> products = productService.getAllProducts();
        int rowNum = 1;
        
        for (Product product : products) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getName());
            row.createCell(2).setCellValue(product.getDescription() != null ? product.getDescription() : "");
            row.createCell(3).setCellValue(product.getCategory().getName());
            row.createCell(4).setCellValue(product.getPrice().doubleValue());
            row.createCell(5).setCellValue(product.getStock());
            row.createCell(6).setCellValue(product.getActive() ? "Activo" : "Inactivo");
            row.createCell(7).setCellValue(product.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        log.info("Reporte Excel de productos generado con {} registros", products.size());
        return baos.toByteArray();
    }

    // RESTO DE MÉTODOS PDF...
    @Transactional(readOnly = true)
    public byte[] generateProductsPdfReport() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Reporte de Productos")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18));

        document.add(new Paragraph("Fecha: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10));

        List<Product> products = productService.getAllProducts();
        
        Table table = new Table(new float[]{2, 3, 2, 1, 1, 2});
        table.setWidth(100);

        table.addHeaderCell(new Cell().add(new Paragraph("ID")));
        table.addHeaderCell(new Cell().add(new Paragraph("Nombre")));
        table.addHeaderCell(new Cell().add(new Paragraph("Categoría")));
        table.addHeaderCell(new Cell().add(new Paragraph("Precio")));
        table.addHeaderCell(new Cell().add(new Paragraph("Stock")));
        table.addHeaderCell(new Cell().add(new Paragraph("Estado")));

        for (Product product : products) {
            table.addCell(new Cell().add(new Paragraph(product.getId().toString())));
            table.addCell(new Cell().add(new Paragraph(product.getName())));
            table.addCell(new Cell().add(new Paragraph(product.getCategory().getName())));
            table.addCell(new Cell().add(new Paragraph("$" + product.getPrice().toString())));
            table.addCell(new Cell().add(new Paragraph(product.getStock().toString())));
            table.addCell(new Cell().add(new Paragraph(product.getActive() ? "Activo" : "Inactivo")));
        }

        document.add(table);

        document.add(new Paragraph("\nEstadísticas:")
                .setFontSize(14));
        document.add(new Paragraph("Total de productos: " + products.size()));
        document.add(new Paragraph("Productos activos: " + productService.getTotalActiveProducts()));
        document.add(new Paragraph("Stock total: " + productService.getTotalStock()));

        document.close();
        log.info("Reporte PDF de productos generado con {} registros", products.size());
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generateCategoriesPdfReport() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Reporte de Categorías")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18));

        document.add(new Paragraph("Fecha: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10));

        List<Category> categories = categoryService.getAllCategories();
        
        Table table = new Table(new float[]{1, 3, 4, 1, 1});
        table.setWidth(100);

        table.addHeaderCell(new Cell().add(new Paragraph("ID")));
        table.addHeaderCell(new Cell().add(new Paragraph("Nombre")));
        table.addHeaderCell(new Cell().add(new Paragraph("Descripción")));
        table.addHeaderCell(new Cell().add(new Paragraph("Productos")));
        table.addHeaderCell(new Cell().add(new Paragraph("Estado")));

        for (Category category : categories) {
            table.addCell(new Cell().add(new Paragraph(category.getId().toString())));
            table.addCell(new Cell().add(new Paragraph(category.getName())));
            table.addCell(new Cell().add(new Paragraph(category.getDescription() != null ? category.getDescription() : "")));
            table.addCell(new Cell().add(new Paragraph(categoryService.getProductCountByCategory(category.getId()).toString())));
            table.addCell(new Cell().add(new Paragraph(category.getActive() ? "Activa" : "Inactiva")));
        }

        document.add(table);

        document.add(new Paragraph("\nEstadísticas:")
                .setFontSize(14));
        document.add(new Paragraph("Total de categorías: " + categories.size()));
        document.add(new Paragraph("Categorías activas: " + categoryService.getAllActiveCategories().size()));

        document.close();
        log.info("Reporte PDF de categorías generado con {} registros", categories.size());
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generateInventoryReport() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Reporte de Inventario")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18));

        document.add(new Paragraph("Fecha: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10));

        List<Product> lowStockProducts = productService.getLowStockProducts(10);
        
        document.add(new Paragraph("Productos con Bajo Stock (menor a 10 unidades)")
                .setFontSize(14));

        Table lowStockTable = new Table(new float[]{3, 2, 1, 1});
        lowStockTable.setWidth(100);

        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Producto")));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Categoría")));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Stock")));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Estado")));

        for (Product product : lowStockProducts) {
            lowStockTable.addCell(new Cell().add(new Paragraph(product.getName())));
            lowStockTable.addCell(new Cell().add(new Paragraph(product.getCategory().getName())));
            lowStockTable.addCell(new Cell().add(new Paragraph(product.getStock().toString())));
            lowStockTable.addCell(new Cell().add(new Paragraph(product.getStock() == 0 ? "Sin Stock" : "Bajo Stock")));
        }

        document.add(lowStockTable);
        document.close();
        return baos.toByteArray();
    }
}