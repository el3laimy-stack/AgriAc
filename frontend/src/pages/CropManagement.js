import React, { useState, useEffect } from 'react';
import { getCrops, createCrop } from '../api/crops';

function CropManagement() {
    const [crops, setCrops] = useState([]);
    const [cropName, setCropName] = useState('');
    // Updated state for conversion factors to handle dynamic rows
    const [conversionFactors, setConversionFactors] = useState([{ unit: '', factor: '' }]);

    useEffect(() => {
        fetchCrops();
    }, []);

    const fetchCrops = async () => {
        try {
            const data = await getCrops();
            setCrops(data);
        } catch (error) {
            console.error("Failed to fetch crops:", error);
        }
    };

    // Handlers for the new dynamic conversion factor inputs
    const handleFactorChange = (index, event) => {
        const values = [...conversionFactors];
        values[index][event.target.name] = event.target.value;
        setConversionFactors(values);
    };

    const handleAddFactor = () => {
        setConversionFactors([...conversionFactors, { unit: '', factor: '' }]);
    };

    const handleRemoveFactor = (index) => {
        const values = [...conversionFactors];
        values.splice(index, 1);
        setConversionFactors(values);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Transform the array of factors into the dictionary format required by the backend
            const factorsObject = conversionFactors.reduce((acc, curr) => {
                if (curr.unit && curr.factor) {
                    acc[curr.unit] = parseFloat(curr.factor);
                }
                return acc;
            }, {});

            // Extract pricing units from the factors
            const allowed_pricing_units = Object.keys(factorsObject);

            const newCrop = { 
                crop_name: cropName, 
                allowed_pricing_units,
                conversion_factors: factorsObject
            };

            await createCrop(newCrop);
            fetchCrops(); // Refresh the list
            
            // Clear form
            setCropName('');
            setConversionFactors([{ unit: '', factor: '' }]);

        } catch (error) {
            console.error("Failed to create crop:", error);
        }
    };

    return (
        <div className="container mt-4">
            <h2>إدارة المحاصيل</h2>
            
            <div className="card mb-4">
                <div className="card-header">إضافة محصول جديد</div>
                <div className="card-body">
                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label htmlFor="cropName" className="form-label">اسم المحصول</label>
                            <input 
                                type="text" 
                                className="form-control"
                                id="cropName"
                                value={cropName}
                                onChange={(e) => setCropName(e.target.value)}
                                required
                            />
                        </div>

                        <hr />

                        <h5>عوامل التحويل (بالنسبة للكيلوجرام)</h5>
                        {conversionFactors.map((factor, index) => (
                            <div className="row align-items-center mb-2" key={index}>
                                <div className="col-md-5">
                                    <input 
                                        type="text"
                                        className="form-control"
                                        name="unit"
                                        placeholder="اسم الوحدة (مثال: شكارة)"
                                        value={factor.unit}
                                        onChange={event => handleFactorChange(index, event)}
                                        required
                                    />
                                </div>
                                <div className="col-md-5">
                                    <input 
                                        type="number"
                                        className="form-control"
                                        name="factor"
                                        placeholder="تساوي كم كيلو؟ (مثال: 50)"
                                        value={factor.factor}
                                        onChange={event => handleFactorChange(index, event)}
                                        required
                                    />
                                </div>
                                <div className="col-md-2">
                                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleRemoveFactor(index)}>حذف</button>
                                </div>
                            </div>
                        ))}
                        
                        <button type="button" className="btn btn-secondary me-2" onClick={handleAddFactor}>+ إضافة وحدة أخرى</button>
                        <button type="submit" className="btn btn-primary">حفظ المحصول</button>
                    </form>
                </div>
            </div>

            <div className="card">
                <div className="card-header">قائمة المحاصيل</div>
                <div className="card-body">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>اسم المحصول</th>
                                <th>وحدات التسعير</th>
                                <th>عوامل التحويل</th>
                            </tr>
                        </thead>
                        <tbody>
                            {crops.map(crop => (
                                <tr key={crop.crop_id}>
                                    <td>{crop.crop_id}</td>
                                    <td>{crop.crop_name}</td>
                                    <td>{crop.allowed_pricing_units.join(', ')}</td>
                                    <td>{JSON.stringify(crop.conversion_factors)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default CropManagement;
