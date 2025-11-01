package accounting.component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Modern Date Range Picker with preset buttons and enhanced UI
 */
public class ModernDateRangePicker extends VBox {
    
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private HBox presetsContainer;
    private Button activePresetButton;
    
    private Consumer<DateRange> onDateRangeChanged;
    
    public ModernDateRangePicker() {
        initialize();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initialize() {
        fromDatePicker = new DatePicker();
        toDatePicker = new DatePicker();
        presetsContainer = new HBox();
        
        // Set default values
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());
        
        // Apply styling
        this.getStyleClass().add("modern-date-section");
        fromDatePicker.getStyleClass().add("smart-search-field");
        toDatePicker.getStyleClass().add("smart-search-field");
        presetsContainer.getStyleClass().add("date-presets");
    }
    
    private void setupLayout() {
        // Header
        Label header = new Label("📅 الفترة الزمنية");
        header.getStyleClass().add("filter-section-header");
        
        // Date pickers container
        HBox datePickersBox = new HBox(12);
        datePickersBox.setAlignment(Pos.CENTER_LEFT);
        
        Label fromLabel = new Label("من:");
        fromLabel.getStyleClass().add("filter-label");
        
        Label toLabel = new Label("إلى:");
        toLabel.getStyleClass().add("filter-label");
        
        datePickersBox.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);
        
        // Preset buttons
        createPresetButtons();
        
        // Add all components
        this.getChildren().addAll(header, presetsContainer, datePickersBox);
        this.setSpacing(12);
    }
    
    private void createPresetButtons() {
        presetsContainer.setAlignment(Pos.CENTER_LEFT);
        presetsContainer.setSpacing(8);
        
        // Create preset buttons
        Button todayBtn = createPresetButton("اليوم", () -> setDateRange(LocalDate.now(), LocalDate.now()));
        Button weekBtn = createPresetButton("هذا الأسبوع", this::setThisWeek);
        Button monthBtn = createPresetButton("هذا الشهر", this::setThisMonth);
        Button quarterBtn = createPresetButton("هذا الربع", this::setThisQuarter);
        Button yearBtn = createPresetButton("هذا العام", this::setThisYear);
        Button customBtn = createPresetButton("مخصص", this::setCustom);
        
        presetsContainer.getChildren().addAll(todayBtn, weekBtn, monthBtn, quarterBtn, yearBtn, customBtn);
        
        // Set month as default active
        setActivePresetButton(monthBtn);
    }
    
    private Button createPresetButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("quick-preset-button");
        
        button.setOnAction(e -> {
            action.run();
            setActivePresetButton(button);
            notifyDateRangeChanged();
        });
        
        return button;
    }
    
    private void setActivePresetButton(Button button) {
        // Remove active class from previous button
        if (activePresetButton != null) {
            activePresetButton.getStyleClass().remove("active");
        }
        
        // Set new active button
        activePresetButton = button;
        button.getStyleClass().add("active");
    }
    
    private void setupEventHandlers() {
        fromDatePicker.setOnAction(e -> {
            clearActivePreset();
            notifyDateRangeChanged();
        });
        
        toDatePicker.setOnAction(e -> {
            clearActivePreset();
            notifyDateRangeChanged();
        });
    }
    
    private void clearActivePreset() {
        if (activePresetButton != null) {
            activePresetButton.getStyleClass().remove("active");
            activePresetButton = null;
        }
    }
    
    // Preset date range methods
    private void setThisWeek() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        setDateRange(startOfWeek, endOfWeek);
    }
    
    private void setThisMonth() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
        setDateRange(startOfMonth, endOfMonth);
    }
    
    private void setThisQuarter() {
        LocalDate now = LocalDate.now();
        int currentQuarter = (now.getMonthValue() - 1) / 3;
        LocalDate startOfQuarter = LocalDate.of(now.getYear(), currentQuarter * 3 + 1, 1);
        LocalDate endOfQuarter = startOfQuarter.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        setDateRange(startOfQuarter, endOfQuarter);
    }
    
    private void setThisYear() {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.with(TemporalAdjusters.lastDayOfYear());
        setDateRange(startOfYear, endOfYear);
    }
    
    private void setCustom() {
        // Custom option - user can select dates manually
        // This just ensures no preset is active
    }
    
    private void setDateRange(LocalDate from, LocalDate to) {
        fromDatePicker.setValue(from);
        toDatePicker.setValue(to);
    }
    
    private void notifyDateRangeChanged() {
        if (onDateRangeChanged != null) {
            DateRange range = new DateRange(fromDatePicker.getValue(), toDatePicker.getValue());
            onDateRangeChanged.accept(range);
        }
    }
    
    // Public API
    public LocalDate getFromDate() {
        return fromDatePicker.getValue();
    }
    
    public LocalDate getToDate() {
        return toDatePicker.getValue();
    }
    
    public void setFromDate(LocalDate date) {
        fromDatePicker.setValue(date);
        clearActivePreset();
    }
    
    public void setToDate(LocalDate date) {
        toDatePicker.setValue(date);
        clearActivePreset();
    }
    
    public void setOnDateRangeChanged(Consumer<DateRange> callback) {
        this.onDateRangeChanged = callback;
    }
    
    public DateRange getDateRange() {
        return new DateRange(fromDatePicker.getValue(), toDatePicker.getValue());
    }
    
    // Inner class for date range
    public static class DateRange {
        private final LocalDate from;
        private final LocalDate to;
        
        public DateRange(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
        }
        
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        
        public boolean isValid() {
            return from != null && to != null && !from.isAfter(to);
        }
        
        @Override
        public String toString() {
            if (from == null || to == null) return "غير محدد";
            if (from.equals(to)) return from.toString();
            return "من " + from + " إلى " + to;
        }
    }
}