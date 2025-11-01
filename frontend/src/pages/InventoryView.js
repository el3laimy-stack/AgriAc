import React, { useState, useEffect } from 'react';
import { getInventory } from '../api/inventory';

function InventoryView() {
    const [inventory, setInventory] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchInventory();
    }, []);

    const fetchInventory = async () => {
        setLoading(true);
        try {
            const data = await getInventory();
            setInventory(data);
        } catch (error) {
            console.error("Failed to fetch inventory:", error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="container mt-4">Loading...</div>;
    }

    return (
        <div className="container mt-4">
            <h2>عرض المخزون</h2>
            <div className="card">
                <div className="card-header">أرصدة المخزون الحالية</div>
                <div className="card-body">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>المحصول</th>
                                <th>الكمية الحالية (كجم)</th>
                                <th>متوسط التكلفة (للكيلو)</th>
                                <th>القيمة الإجمالية للمخزون</th>
                            </tr>
                        </thead>
                        <tbody>
                            {inventory.map(item => (
                                <tr key={item.crop.crop_id}>
                                    <td>{item.crop.crop_name}</td>
                                    <td>{item.current_stock_kg.toFixed(2)}</td>
                                    <td>{item.average_cost_per_kg.toFixed(2)}</td>
                                    <td>{(item.current_stock_kg * item.average_cost_per_kg).toFixed(2)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default InventoryView;
