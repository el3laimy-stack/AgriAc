package accounting.component;

import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Smart Search Field with icon and enhanced functionality
 */
public class SmartSearchField extends VBox {
    
    private TextField searchField;
    private HBox searchContainer;
    private FontIcon searchIcon;
    private Consumer<String> onSearchChanged;
    private ObservableList<String> searchHistory;
    
    public SmartSearchField(String title, String placeholder) {
        this.searchHistory = FXCollections.observableArrayList();
        initialize(title, placeholder);
        setupLayout();
        setupEventHandlers();
    }
    
    private void initialize(String title, String placeholder) {
        // Create search field
        searchField = new TextField();
        searchField.setPromptText(placeholder);
        searchField.getStyleClass().add("smart-search-field");
        
        // Create search icon
        searchIcon = new FontIcon("fa-search");
        searchIcon.getStyleClass().add("search-icon");
        searchIcon.setIconSize(16);
        
        // Create search container
        searchContainer = new HBox(12);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        
        this.getStyleClass().add("modern-date-section");
        this.setSpacing(12);
    }
    
    private void setupLayout() {
        // Header
        Label header = new Label("🔍 البحث النصي");
        header.getStyleClass().add("filter-section-header");
        
        // Search container with icon and field
        searchContainer.getChildren().addAll(searchIcon, searchField);
        
        // Add components
        this.getChildren().addAll(header, searchContainer);
    }
    
    private void setupEventHandlers() {
        // Real-time search
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (onSearchChanged != null) {
                onSearchChanged.accept(newValue);
            }
            
            // Add to history if not empty and not already present
            if (newValue != null && !newValue.trim().isEmpty() && !searchHistory.contains(newValue)) {
                searchHistory.add(0, newValue); // Add to beginning
                
                // Keep only last 10 searches
                if (searchHistory.size() > 10) {
                    searchHistory.remove(searchHistory.size() - 1);
                }
            }
        });
        
        // Focus/unfocus styling
        searchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                searchIcon.getStyleClass().add("focused");
            } else {
                searchIcon.getStyleClass().remove("focused");
            }
        });
    }
    
    // Public API
    public String getSearchText() {
        return searchField.getText();
    }
    
    public void setSearchText(String text) {
        searchField.setText(text);
    }
    
    public void clearSearch() {
        searchField.clear();
    }
    
    public void setOnSearchChanged(Consumer<String> callback) {
        this.onSearchChanged = callback;
    }
    
    public ObservableList<String> getSearchHistory() {
        return searchHistory;
    }
    
    public void setPromptText(String promptText) {
        searchField.setPromptText(promptText);
    }
    
    public boolean isEmpty() {
        return searchField.getText() == null || searchField.getText().trim().isEmpty();
    }
    
    public boolean hasText() {
        return !isEmpty();
    }
}