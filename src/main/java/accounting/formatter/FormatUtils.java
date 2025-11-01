package accounting.formatter;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.TableCell;

public class FormatUtils {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatCurrency(double value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatDateForDisplay(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String formatNumber(Double number) {
        if (number == null) return "";
        return String.format("%.2f", number);
    }


    public static String formatQuantityWithUnit(double quantity, String unit) {
        return String.format("%.2f %s", quantity, unit);
    }

    public static String formatDateForDatabase(LocalDate date) {
        if (date == null) return null;
        return date.format(DATE_FORMATTER);
    }

    public static LocalDate parseDateFromDatabase(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }

    public static <T> TableCell<T, Double> createCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatCurrency(item));
                }
            }
        };
    }
}
