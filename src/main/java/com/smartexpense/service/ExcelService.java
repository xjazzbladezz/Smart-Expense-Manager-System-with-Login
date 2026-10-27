package com.smartexpense.service;

import com.smartexpense.model.Categories;
import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Excel (.xlsx) import and export using Apache POI. */
public final class ExcelService {

    public record ImportResult(List<Transaction> imported, List<String> errors) {
    }

    private static final String[] HEADERS = {"Date", "Description", "Category", "Payment Mode", "Type", "Amount"};

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    };

    private ExcelService() {
    }

    // ================================================================== EXPORT

    public static void export(List<Transaction> list, File file) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle head = headerStyle(wb);
            XSSFCellStyle dateStyle = bodyStyle(wb, "dd-mmm-yyyy", HorizontalAlignment.LEFT);
            XSSFCellStyle textStyle = bodyStyle(wb, null, HorizontalAlignment.LEFT);
            XSSFCellStyle moneyStyle = bodyStyle(wb, "#,##0.00", HorizontalAlignment.RIGHT);

            Sheet sheet = wb.createSheet("Transactions");
            Row hr = sheet.createRow(0);
            hr.setHeightInPoints(24);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(head);
            }

            int r = 1;
            for (Transaction t : list) {
                Row row = sheet.createRow(r++);
                Cell c0 = row.createCell(0);
                c0.setCellValue(java.sql.Date.valueOf(t.getDate()));
                c0.setCellStyle(dateStyle);
                set(row, 1, t.getDescription() == null ? "" : t.getDescription(), textStyle);
                set(row, 2, t.getCategory(), textStyle);
                set(row, 3, t.getPaymentMode(), textStyle);
                set(row, 4, t.getType().label(), textStyle);
                Cell amt = row.createCell(5);
                amt.setCellValue(t.getAmount());
                amt.setCellStyle(moneyStyle);
            }
            sheet.createFreezePane(0, 1);
            if (r > 1) sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, r - 1, 0, 5));
            int[] widths = {14, 38, 20, 16, 12, 16};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            buildSummary(wb, list, head, textStyle, moneyStyle, r - 1);

            try (OutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    private static void buildSummary(XSSFWorkbook wb, List<Transaction> list, XSSFCellStyle head,
                                     XSSFCellStyle text, XSSFCellStyle money, int lastDataRow) {
        Sheet s = wb.createSheet("Summary");
        Row h = s.createRow(0);
        h.setHeightInPoints(24);
        String[] cols = {"Category", "Type", "Total (live formula)"};
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(head);
        }
        Map<String, TransactionType> cats = new TreeMap<>();
        for (Transaction t : list) cats.put(t.getCategory(), t.getType());
        int r = 1;
        int last = Math.max(lastDataRow + 1, 2);
        for (Map.Entry<String, TransactionType> e : cats.entrySet()) {
            Row row = s.createRow(r);
            set(row, 0, e.getKey(), text);
            set(row, 1, e.getValue().label(), text);
            Cell f = row.createCell(2);
            f.setCellFormula("SUMIFS(Transactions!$F$2:$F$" + last + ",Transactions!$C$2:$C$" + last
                    + ",A" + (r + 1) + ",Transactions!$E$2:$E$" + last + ",B" + (r + 1) + ")");
            f.setCellStyle(money);
            r++;
        }
        r++;
        Row inc = s.createRow(r++);
        set(inc, 0, "Total Income", text);
        Cell ic = inc.createCell(2);
        ic.setCellFormula("SUMIFS(Transactions!$F$2:$F$" + last + ",Transactions!$E$2:$E$" + last + ",\"Income\")");
        ic.setCellStyle(money);
        Row exp = s.createRow(r++);
        set(exp, 0, "Total Expense", text);
        Cell ec = exp.createCell(2);
        ec.setCellFormula("SUMIFS(Transactions!$F$2:$F$" + last + ",Transactions!$E$2:$E$" + last + ",\"Expense\")");
        ec.setCellStyle(money);
        Row bal = s.createRow(r);
        set(bal, 0, "Net Balance", text);
        Cell bc = bal.createCell(2);
        bc.setCellFormula("C" + (r - 1) + "-C" + r);
        bc.setCellStyle(money);
        s.setColumnWidth(0, 28 * 256);
        s.setColumnWidth(1, 14 * 256);
        s.setColumnWidth(2, 24 * 256);
        wb.setActiveSheet(0);
    }

    public static void writeTemplate(File file) throws IOException {
        List<Transaction> sample = new ArrayList<>();
        LocalDate d = LocalDate.now();
        sample.add(new Transaction(0, 0, d.withDayOfMonth(1), TransactionType.INCOME, "Salary", 85000,
                "Monthly salary", "Net Banking"));
        sample.add(new Transaction(0, 0, d.withDayOfMonth(2), TransactionType.EXPENSE, "Rent & Housing", 22000,
                "House rent", "UPI"));
        sample.add(new Transaction(0, 0, d.withDayOfMonth(3), TransactionType.EXPENSE, "Food & Dining", 480,
                "Dinner at Biryani House", "Credit Card"));
        export(sample, file);
    }

    // ================================================================== IMPORT

    public static ImportResult importFile(File file) throws IOException {
        List<Transaction> ok = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();

        try (InputStream in = new FileInputStream(file); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                errors.add("The first sheet is empty.");
                return new ImportResult(ok, errors);
            }
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) {
                col.put(fmt.formatCellValue(c).trim().toLowerCase(), c.getColumnIndex());
            }
            Integer cDate = col.get("date");
            Integer cDesc = col.getOrDefault("description", col.get("note"));
            Integer cCat = col.get("category");
            Integer cMode = col.getOrDefault("payment mode", col.get("mode"));
            Integer cType = col.get("type");
            Integer cAmt = col.get("amount");
            if (cDate == null || cCat == null || cType == null || cAmt == null) {
                errors.add("Header row must contain: Date, Category, Type, Amount "
                        + "(optional: Description, Payment Mode).");
                return new ImportResult(ok, errors);
            }

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String line = "Row " + (r + 1) + ": ";
                try {
                    if (isBlank(row, fmt)) continue;
                    LocalDate date = readDate(row.getCell(cDate), fmt);
                    if (date == null) {
                        errors.add(line + "invalid date");
                        continue;
                    }
                    TransactionType type = TransactionType.parse(fmt.formatCellValue(row.getCell(cType)));
                    if (type == null) {
                        errors.add(line + "type must be Income or Expense");
                        continue;
                    }
                    Double amount = readAmount(row.getCell(cAmt), fmt);
                    if (amount == null || amount <= 0) {
                        errors.add(line + "invalid amount");
                        continue;
                    }
                    String category = fmt.formatCellValue(row.getCell(cCat)).trim();
                    if (category.isEmpty()) category = type == TransactionType.INCOME ? "Other Income" : "Other";
                    String desc = cDesc == null ? "" : fmt.formatCellValue(row.getCell(cDesc)).trim();
                    String mode = cMode == null ? "UPI" : fmt.formatCellValue(row.getCell(cMode)).trim();
                    if (mode.isEmpty()) mode = "UPI";
                    mode = matchMode(mode);
                    ok.add(new Transaction(0, 0, date, type, category, amount, desc, mode));
                } catch (RuntimeException ex) {
                    errors.add(line + "could not be read (" + ex.getMessage() + ")");
                }
            }
        }
        return new ImportResult(ok, errors);
    }

    private static String matchMode(String mode) {
        for (String m : Categories.PAYMENT_MODES) {
            if (m.equalsIgnoreCase(mode)) return m;
        }
        return mode;
    }

    private static boolean isBlank(Row row, DataFormatter fmt) {
        for (Cell c : row) {
            if (c.getCellType() != CellType.BLANK && !fmt.formatCellValue(c).trim().isEmpty()) return false;
        }
        return true;
    }

    private static LocalDate readDate(Cell c, DataFormatter fmt) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = fmt.formatCellValue(c).trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static Double readAmount(Cell c, DataFormatter fmt) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        String s = fmt.formatCellValue(c).replace("₹", "").replace(",", "").replace("Rs.", "").trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ================================================================== styles

    private static void set(Row row, int col, String value, XSSFCellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private static XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x5B, (byte) 0x3F, (byte) 0xE0}, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        f.setFontHeightInPoints((short) 11);
        st.setFont(f);
        return st;
    }

    private static XSSFCellStyle bodyStyle(XSSFWorkbook wb, String numberFormat, HorizontalAlignment align) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setAlignment(align);
        st.setBorderBottom(BorderStyle.HAIR);
        st.setBottomBorderColor(new XSSFColor(new byte[]{(byte) 0xBB, (byte) 0xBB, (byte) 0xBB}, null));
        if (numberFormat != null) {
            CreationHelper ch = wb.getCreationHelper();
            DataFormat df = wb.createDataFormat();
            st.setDataFormat(df.getFormat(numberFormat));
            if (ch == null) throw new IllegalStateException();
        }
        return st;
    }
}
