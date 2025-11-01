package accounting.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class UnitFactor {
    private final SimpleStringProperty unitName;
    private final SimpleDoubleProperty conversionFactor;

    public UnitFactor(String unitName, Double conversionFactor) {
        this.unitName = new SimpleStringProperty(unitName);
        this.conversionFactor = new SimpleDoubleProperty(conversionFactor);
    }

    public String getUnitName() {
        return unitName.get();
    }

    public SimpleStringProperty unitNameProperty() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName.set(unitName);
    }

    public double getConversionFactor() {
        return conversionFactor.get();
    }

    public SimpleDoubleProperty conversionFactorProperty() {
        return conversionFactor;
    }

    public void setConversionFactor(double conversionFactor) {
        this.conversionFactor.set(conversionFactor);
    }
}