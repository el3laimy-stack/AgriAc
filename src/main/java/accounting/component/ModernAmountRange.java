package accounting.component;

import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Modern Amount Range Component with preset values
 */
public class ModernAmountRange extends VBox {
    
    private TextField minAmountField;
    private TextField maxAmountField;
    private HBox presetsContainer;
    private Consumer<AmountRange> onRangeChanged;
    
    public ModernAmountRange() {
        initialize();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initialize() {
        minAmountField = new TextField();
        maxAmountField = new TextField();
        presetsContainer = new HBox();
        
        // Set prompt text
        minAmountField.setPromptText("الحد الأدنى");
        maxAmountField.setPromptText("الحد الأقصى");
        
        // Apply styling
        this.getStyleClass().add("modern-date-section");
        minAmountField.getStyleClass().add("amount-input");
        maxAmountField.getStyleClass().add("amount-input");
        presetsContainer.getStyleClass().add("date-presets");
    }
    
    private void setupLayout() {
        // Header
        Label header = new Label("💰 نطاق المبلغ");
        header.getStyleClass().add("filter-section-header");
        
        // Amount inputs container
        HBox amountInputsBox = new HBox(12);
        amountInputsBox.setAlignment(Pos.CENTER_LEFT);
        amountInputsBox.getStyleClass().add("amount-range-container");
        
        Label fromLabel = new Label("من:");
        fromLabel.getStyleClass().add("filter-label");
        
        Label toLabel = new Label("إلى:");
        toLabel.getStyleClass().add("filter-label");
        
        amountInputsBox.getChildren().addAll(fromLabel, minAmountField, toLabel, maxAmountField);
        
        // Preset buttons
        createPresetButtons();
        
        // Add all components
        this.getChildren().addAll(header, presetsContainer, amountInputsBox);
        this.setSpacing(12);
    }
    
    private void createPresetButtons() {
        presetsContainer.setAlignment(Pos.CENTER_LEFT);
        presetsContainer.setSpacing(8);
        
        // Create preset buttons with common amount ranges
        Button range1 = createPresetButton("أقل من 100", () -> setAmountRange(null, 100.0));
        Button range2 = createPresetButton("100 - 500", () -> setAmountRange(100.0, 500.0));
        Button range3 = createPresetButton("500 - 1000", () -> setAmountRange(500.0, 1000.0));
        Button range4 = createPresetButton("1000 - 5000", () -> setAmountRange(1000.0, 5000.0));
        Button range5 = createPresetButton("أكثر من 5000", () -> setAmountRange(5000.0, null));
        Button clearBtn = createPresetButton("مسح", () -> clearAmountRange());
        
        presetsContainer.getChildren().addAll(range1, range2, range3, range4, range5, clearBtn);
    }
    
    private Button createPresetButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("quick-preset-button");
        
        button.setOnAction(e -> {
            action.run();
            notifyRangeChanged();
        });
        
        return button;
    }
    
    private void setupEventHandlers() {
        // Add number validation
        minAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isValidNumber(newValue)) {
                minAmountField.setText(oldValue);
            } else {
                notifyRangeChanged();
            }
        });
        
        maxAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isValidNumber(newValue)) {
                maxAmountField.setText(oldValue);
            } else {
                notifyRangeChanged();
            }
        });
    }
    
    private boolean isValidNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true; // Empty is valid
        }
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void setAmountRange(Double min, Double max) {
        minAmountField.setText(min != null ? String.valueOf(min) : "");
        maxAmountField.setText(max != null ? String.valueOf(max) : "");
    }
    
    private void clearAmountRange() {
        minAmountField.clear();
        maxAmountField.clear();
    }
    
    private void notifyRangeChanged() {
        if (onRangeChanged != null) {
            AmountRange range = getAmountRange();
            onRangeChanged.accept(range);
        }
    }
    
    // Public API
    public AmountRange getAmountRange() {
        Double min = parseAmount(minAmountField.getText());
        Double max = parseAmount(maxAmountField.getText());
        return new AmountRange(min, max);
    }
    
    private Double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public void setMinAmount(Double amount) {
        minAmountField.setText(amount != null ? String.valueOf(amount) : "");
    }
    
    public void setMaxAmount(Double amount) {
        maxAmountField.setText(amount != null ? String.valueOf(amount) : "");
    }
    
    public void setAmountRange(AmountRange range) {
        setMinAmount(range.getMin());
        setMaxAmount(range.getMax());
    }
    
    public void clearRange() {
        clearAmountRange();
        notifyRangeChanged();
    }
    
    public void setOnRangeChanged(Consumer<AmountRange> callback) {
        this.onRangeChanged = callback;
    }
    
    public boolean hasRange() {
        AmountRange range = getAmountRange();
        return range.getMin() != null || range.getMax() != null;
    }
    
    // Amount Range class
    public static class AmountRange {
        private final Double min;
        private final Double max;
        
        public AmountRange(Double min, Double max) {
            this.min = min;
            this.max = max;
        }
        
        public Double getMin() { return min; }
        public Double getMax() { return max; }
        
        public boolean isValid() {
            if (min != null && max != null) {
                return min <= max;
            }
            return true; // Single-sided ranges are valid
        }
        
        public boolean isInRange(double value) {
            if (min != null && value < min) return false;
            if (max != null && value > max) return false;
            return true;
        }
        
        public boolean isEmpty() {
            return min == null && max == null;
        }
        
        @Override
        public String toString() {
            if (min == null && max == null) return "غير محدد";
            if (min == null) return "أقل من " + max;
            if (max == null) return "أكثر من " + min;
            if (min.equals(max)) return String.valueOf(min);
            return "من " + min + " إلى " + max;
        }
    }
}