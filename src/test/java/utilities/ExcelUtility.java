package utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Stateless per-operation workbook access; callers must serialize writes to the same file. */
public final class ExcelUtility {
    private final Path workbookPath;

    public ExcelUtility(String path) {
        workbookPath = Path.of(path);
    }

    /** Legacy API: last zero-based row index (header is row zero); -1 for an empty sheet. */
    public int getRowCount(String sheetName) throws IOException {
        try (XSSFWorkbook workbook = openWorkbook()) {
            Sheet sheet = getRequiredSheet(workbook, sheetName);
            return sheet.getPhysicalNumberOfRows() == 0 ? -1 : sheet.getLastRowNum();
        }
    }

    public int getCellCount(String sheetName, int rowIndex) throws IOException {
        try (XSSFWorkbook workbook = openWorkbook()) {
            Row row = getRequiredSheet(workbook, sheetName).getRow(rowIndex);
            return row == null ? 0 : Math.max(0, row.getLastCellNum());
        }
    }

    public String getCellData(String sheetName, int rowIndex, int columnIndex) throws IOException {
        try (XSSFWorkbook workbook = openWorkbook()) {
            Row row = getRequiredSheet(workbook, sheetName).getRow(rowIndex);
            Cell cell = row == null ? null : row.getCell(columnIndex);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return formatter.formatCellValue(cell, evaluator);
        }
    }

    /** Reads once and preserves row positions and empty cells, including the header. */
    public List<List<String>> readRows(String sheetName) throws IOException {
        try (XSSFWorkbook workbook = openWorkbook()) {
            Sheet sheet = getRequiredSheet(workbook, sheetName);
            List<List<String>> rows = new ArrayList<>();
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return rows;
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> cellValues = new ArrayList<>();
                if (row != null) {
                    for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                        cellValues.add(formatter.formatCellValue(row.getCell(columnIndex), evaluator));
                    }
                }
                rows.add(cellValues);
            }
            return rows;
        }
    }

    public void setCellData(String sheetName, int rowIndex, int columnIndex, String value) throws IOException {
        try (XSSFWorkbook workbook = Files.exists(workbookPath) ? openWorkbook() : new XSSFWorkbook()) {
            getOrCreateCell(workbook, sheetName, rowIndex, columnIndex).setCellValue(value);
            saveWorkbook(workbook);
        }
    }

    public void fillGreenColor(String sheetName, int rowIndex, int columnIndex) throws IOException {
        fillCell(sheetName, rowIndex, columnIndex, IndexedColors.GREEN);
    }

    public void fillRedColor(String sheetName, int rowIndex, int columnIndex) throws IOException {
        fillCell(sheetName, rowIndex, columnIndex, IndexedColors.RED);
    }

    private XSSFWorkbook openWorkbook() throws IOException {
        try (InputStream input = Files.newInputStream(workbookPath)) {
            return new XSSFWorkbook(input);
        }
    }

    private Sheet getRequiredSheet(XSSFWorkbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("Missing worksheet '" + sheetName + "' in " + workbookPath);
        }
        return sheet;
    }

    private Cell getOrCreateCell(XSSFWorkbook workbook, String sheetName, int rowIndex, int columnIndex) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
        }

        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private void saveWorkbook(XSSFWorkbook workbook) throws IOException {
        Path parentDirectory = workbookPath.toAbsolutePath().getParent();
        Files.createDirectories(parentDirectory);

        // Write separately first so a failed workbook serialization cannot truncate the source.
        Path temporaryFile = Files.createTempFile(parentDirectory, "workbook-", ".xlsx");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                workbook.write(output);
            }
            Files.move(temporaryFile, workbookPath.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void fillCell(String sheetName, int rowIndex, int columnIndex, IndexedColors color) throws IOException {
        try (XSSFWorkbook workbook = openWorkbook()) {
            Cell cell = getOrCreateCell(workbook, sheetName, rowIndex, columnIndex);
            CellStyle style = workbook.createCellStyle();
            style.cloneStyleFrom(cell.getCellStyle());
            style.setFillForegroundColor(color.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
            saveWorkbook(workbook);
        }
    }
}
