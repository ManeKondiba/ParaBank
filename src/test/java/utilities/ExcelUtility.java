package utilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Immutable read-only Excel utility.
 * Guarantees zero side-effects on disk: does not write, mutate, or lock workbooks during test execution.
 */
public final class ExcelUtility {
    private final Path workbookPath;

    public ExcelUtility(String path) {
        this.workbookPath = Path.of(path);
    }

    /** Last zero-based row index (header is row zero); -1 for an empty sheet. */
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

    /** Reads all rows preserving positions and empty cells, including the header. */
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

    private XSSFWorkbook openWorkbook() throws IOException {
        if (!Files.exists(workbookPath)) {
            throw new IllegalArgumentException("Workbook does not exist: " + workbookPath);
        }
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
}
