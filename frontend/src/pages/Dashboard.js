import React, { useState, useEffect } from 'react';
import { getDashboardKpis } from '../api/reports';
import KpiCard from '../components/KpiCard';

function Dashboard() {
    const [kpis, setKpis] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchKpis();
    }, []);

    const fetchKpis = async () => {
        setLoading(true);
        try {
            const data = await getDashboardKpis();
            setKpis(data);
        } catch (error) {
            console.error("Failed to fetch KPIs:", error);
        } finally {
            setLoading(false);
        }
    };

    if (loading || !kpis) {
        return <div className="container mt-4">Loading...</div>;
    }

    return (
        <div className="container mt-4">
            <h2>لوحة التحكم</h2>
            <div className="row mt-4">
                <KpiCard title="إجمالي الإيرادات" value={kpis.total_revenue} />
                <KpiCard title="الربح الإجمالي" value={kpis.gross_profit} />
                <KpiCard title="قيمة المخزون" value={kpis.inventory_value} />
                <KpiCard title="عدد المبيعات" value={kpis.sales_count} formatAsCurrency={false} />
            </div>
        </div>
    );
}

export default Dashboard;
