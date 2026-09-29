package utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.DataProvider;

public final class DataProviders {
    public static final String FRESH_ACCOUNT = "__REGISTER_FRESH_ACCOUNT__";
    private static final List<String> LOGIN_HEADERS = List.of("username", "password", "expected");

    @DataProvider(name = "LoginData")
    public String[][] getData() throws IOException {
        return loginData(FrameworkConfig.load());
    }

    public static boolean isFreshAccount(String username, String password, String expected) {
        return "Valid".equalsIgnoreCase(expected)
                && FRESH_ACCOUNT.equals(username)
                && FRESH_ACCOUNT.equals(password);
    }

    public static String[][] loginData(FrameworkConfig config) throws IOException {
        String workbookPath = config.get("testdata.login.path");
        if (workbookPath.isBlank()) {
            String unknownUser = "missing" + UUID.randomUUID().toString().replace("-", "");
            return new String[][] {
                {FRESH_ACCOUNT, FRESH_ACCOUNT, "Valid"},
                {unknownUser, "wrong-password", "Invalid"},
                {"", "", "Invalid"},
                {unknownUser, "", "Invalid"},
                {"", "wrong-password", "Invalid"}
            };
        }
        ExcelUtility workbook = new ExcelUtility(workbookPath);
        String sheetName = config.get("testdata.login.sheet");
        return validateRows(workbook.readRows(sheetName));
    }

    public static String[][] validateRows(List<List<String>> rows) {
        validateHeaders(rows);

        List<String[]> loginData = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            int rowNumber = rowIndex + 1;
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (row.size() != LOGIN_HEADERS.size()) {
                throw new IllegalArgumentException("Login workbook row " + rowNumber + " must have three cells");
            }

            String username = row.get(0);
            String password = row.get(1);
            String expected = row.get(2).trim();
            boolean validLogin = "Valid".equalsIgnoreCase(expected);
            if (!validLogin && !"Invalid".equalsIgnoreCase(expected)) {
                throw new IllegalArgumentException("Login workbook row " + rowNumber + " expected must be Valid or Invalid");
            }
            if (FRESH_ACCOUNT.equals(username) || FRESH_ACCOUNT.equals(password)) {
                throw new IllegalArgumentException("Login workbook contains a reserved account placeholder at row " + rowNumber);
            }
            if (validLogin && (username.isBlank() || password.isBlank())) {
                throw new IllegalArgumentException("Valid login requires nonblank credentials at row " + rowNumber);
            }
            loginData.add(new String[] {username, password, validLogin ? "Valid" : "Invalid"});
        }

        if (loginData.isEmpty()) {
            throw new IllegalArgumentException("Login workbook contains no test cases");
        }
        return loginData.toArray(String[][]::new);
    }

    private static void validateHeaders(List<List<String>> rows) {
        if (rows.isEmpty() || rows.get(0).size() != LOGIN_HEADERS.size()) {
            throw new IllegalArgumentException("Login workbook must have three columns: username, password, expected");
        }

        List<String> headers = rows.get(0);
        for (int columnIndex = 0; columnIndex < LOGIN_HEADERS.size(); columnIndex++) {
            String header = headers.get(columnIndex).trim();
            boolean legacyResultHeader = columnIndex == 2 && "res".equalsIgnoreCase(header);
            if (!LOGIN_HEADERS.get(columnIndex).equalsIgnoreCase(header) && !legacyResultHeader) {
                throw new IllegalArgumentException("Unexpected login workbook header at column " + (columnIndex + 1));
            }
        }
    }
}
