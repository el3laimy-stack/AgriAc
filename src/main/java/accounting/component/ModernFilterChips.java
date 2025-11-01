package accounting.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Filter Chips Component for category selection
 */
public class ModernFilterChips extends VBox {
    
    private FlowPane chipsContainer;
    private ObservableList<FilterOption> availableOptions;
    private ObservableList<FilterOption> selectedOptions;
    private Consumer<List<FilterOption>> onSelectionChanged;
    
    public ModernFilterChips(String title) {
        this.availableOptions = FXCollections.observableArrayList();
        this.selectedOptions = FXCollections.observableArrayList();
        
        initialize(title);
        setupLayout();
    }
    
    private void initialize(String title) {
        chipsContainer = new FlowPane();
        chipsContainer.getStyleClass().add("filter-chip-container");
        chipsContainer.setHgap(8);
        chipsContainer.setVgap(8);
        chipsContainer.setAlignment(Pos.CENTER_LEFT);
        
        this.getStyleClass().add("modern-date-section");
        this.setSpacing(12);
    }
    
    private void setupLayout() {
        // Header with title
        Label header = new Label("🏷️ التصنيفات");
        header.getStyleClass().add("filter-section-header");
        
        // Add components
        this.getChildren().addAll(header, chipsContainer);
    }
    
    public void setOptions(List<FilterOption> options) {
        availableOptions.setAll(options);
        refreshChips();
    }
    
    public void addOption(FilterOption option) {
        availableOptions.add(option);
        refreshChips();
    }
    
    private void refreshChips() {
        chipsContainer.getChildren().clear();
        
        for (FilterOption option : availableOptions) {
            Button chip = createChip(option);
            chipsContainer.getChildren().add(chip);
        }
    }
    
    private Button createChip(FilterOption option) {
        Button chip = new Button();
        
        // Create chip content
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);
        
        // Add icon if present
        if (option.getIcon() != null && !option.getIcon().isEmpty()) {
            FontIcon icon = new FontIcon(option.getIcon());
            icon.setIconSize(14);
            content.getChildren().add(icon);
        }
        
        // Add text
        Label text = new Label(option.getLabel());
        content.getChildren().add(text);
        
        // Add remove icon for selected chips
        if (selectedOptions.contains(option)) {
            FontIcon removeIcon = new FontIcon("fa-times");
            removeIcon.getStyleClass().add("remove-icon");
            removeIcon.setStyle("-fx-font-family: FontAwesome;");
            removeIcon.setIconSize(12);
            content.getChildren().add(removeIcon);
        }
        
        chip.setGraphic(content);
        chip.getStyleClass().add("filter-chip");
        
        // Set selected state
        if (selectedOptions.contains(option)) {
            chip.getStyleClass().add("selected");
        }
        
        // Tooltip for better UX
        Tooltip.install(chip, new Tooltip("تصفية حسب: " + option.getLabel()));
        
        // Visual feedback on hover
        chip.setOnMouseEntered(e -> chip.setStyle("-fx-background-color: #e3f2fd;"));
        chip.setOnMouseExited(e -> chip.setStyle(""));
        
        // Handle click
        chip.setOnAction(e -> toggleSelection(option));
        
        return chip;
    }
    
    private void toggleSelection(FilterOption option) {
        if (selectedOptions.contains(option)) {
            selectedOptions.remove(option);
        } else {
            selectedOptions.add(option);
        }
        
        refreshChips();
        notifySelectionChanged();
    }
    
    private void notifySelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(new ArrayList<>(selectedOptions));
        }
    }
    
    // Public API
    public List<FilterOption> getSelectedOptions() {
        return new ArrayList<>(selectedOptions);
    }
    
    public void setSelectedOptions(List<FilterOption> options) {
        selectedOptions.clear();
        selectedOptions.addAll(options);
        refreshChips();
    }
    
    public void clearSelection() {
        selectedOptions.clear();
        refreshChips();
        notifySelectionChanged();
    }
    
    public void selectAll() {
        selectedOptions.clear();
        selectedOptions.addAll(availableOptions);
        refreshChips();
        notifySelectionChanged();
    }
    
    public void setOnSelectionChanged(Consumer<List<FilterOption>> callback) {
        this.onSelectionChanged = callback;
    }
    
    public boolean hasSelection() {
        return !selectedOptions.isEmpty();
    }
    
    // Filter Option class
    public static class FilterOption {
        private final String value;
        private final String label;
        private final String icon;
        private final String color;
        
        public FilterOption(String value, String label) {
            this(value, label, null, null);
        }
        
        public FilterOption(String value, String label, String icon) {
            this(value, label, icon, null);
        }
        
        public FilterOption(String value, String label, String icon, String color) {
            this.value = value;
            this.label = label;
            this.icon = icon;
            this.color = color;
        }
        
        public String getValue() { return value; }
        public String getLabel() { return label; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FilterOption that = (FilterOption) obj;
            return value.equals(that.value);
        }
        
        @Override
        public int hashCode() {
            return value.hashCode();
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
}