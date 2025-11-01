package accounting.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import java.time.LocalDate;

/**
 * Utility class for form validation in the accounting application.
 * Provides reusable validation methods for common form fields.
 */
public class FormValidator {
    
    private StringBuilder errorMessage;
    
    public FormValidator() {
        this.errorMessage = new StringBuilder();
    }
    
    /**
     * Checks if a required text field is valid (not empty)
     * @param field The text field to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateRequiredTextField(TextField field, String fieldName) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            errorMessage.append(fieldName).append(" مطلوب.\n");
        }
        return this;
    }
    
    /**
     * Checks if a text field contains a valid number
     * @param field The text field to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateNumericField(TextField field, String fieldName) {
        if (field != null && field.getText() != null && !field.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(field.getText().trim());
            } catch (NumberFormatException e) {
                errorMessage.append(fieldName).append(" يجب أن يكون رقماً صحيحاً.\n");
            }
        }
        return this;
    }
    
    /**
     * Checks if a text field contains a valid positive number
     * @param field The text field to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validatePositiveNumericField(TextField field, String fieldName) {
        if (field != null && field.getText() != null && !field.getText().trim().isEmpty()) {
            try {
                double value = Double.parseDouble(field.getText().trim());
                if (value <= 0) {
                    errorMessage.append(fieldName).append(" يجب أن يكون أكبر من صفر.\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append(fieldName).append(" يجب أن يكون رقماً صحيحاً.\n");
            }
        }
        return this;
    }
    
    /**
     * Checks if a required date picker has a valid date
     * @param datePicker The date picker to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateRequiredDatePicker(DatePicker datePicker, String fieldName) {
        if (datePicker == null || datePicker.getValue() == null) {
            errorMessage.append(fieldName).append(" مطلوب.\n");
        }
        return this;
    }
    
    /**
     * Checks if a date picker has a date that is not in the future
     * @param datePicker The date picker to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateNotFutureDatePicker(DatePicker datePicker, String fieldName) {
        if (datePicker != null && datePicker.getValue() != null) {
            if (datePicker.getValue().isAfter(LocalDate.now())) {
                errorMessage.append(fieldName).append(" لا يمكن أن يكون في المستقبل.\n");
            }
        }
        return this;
    }
    
    /**
     * Checks if a required combo box has a selected value
     * @param comboBox The combo box to validate
     * @param fieldName The name of the field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateRequiredComboBox(ComboBox<?> comboBox, String fieldName) {
        if (comboBox == null || comboBox.getValue() == null) {
            errorMessage.append(fieldName).append(" مطلوب.\n");
        }
        return this;
    }
    
    /**
     * Validates that one numeric field is not greater than another
     * @param field1 The first field (e.g., amount paid)
     * @param field2 The second field (e.g., total amount)
     * @param fieldName1 Name of the first field for error messages
     * @param fieldName2 Name of the second field for error messages
     * @return true if valid, false otherwise
     */
    public FormValidator validateFieldNotGreaterThan(TextField field1, TextField field2, String fieldName1, String fieldName2) {
        if (field1 != null && field2 != null && 
            field1.getText() != null && !field1.getText().trim().isEmpty() &&
            field2.getText() != null && !field2.getText().trim().isEmpty()) {
            
            try {
                double value1 = Double.parseDouble(field1.getText().trim());
                double value2 = Double.parseDouble(field2.getText().trim());
                
                if (value1 > value2) {
                    errorMessage.append(fieldName1).append(" لا يمكن أن يكون أكبر من ").append(fieldName2).append(".\n");
                }
            } catch (NumberFormatException e) {
                // If we can't parse the numbers, we skip this validation
                // The numeric validation should catch this case
            }
        }
        return this;
    }
    
    /**
     * Gets the accumulated error messages
     * @return The error messages as a string
     */
    public String getErrorMessage() {
        return errorMessage.toString();
    }
    
    /**
     * Checks if there are any validation errors
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return errorMessage.length() > 0;
    }
    
    /**
     * Clears all validation errors
     */
    public void clearErrors() {
        errorMessage.setLength(0);
    }
}