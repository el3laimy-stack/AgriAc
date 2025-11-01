import React, { useState, useEffect } from 'react';
import { getSales, createSale } from '../api/sales';
import { useData } from '../context/DataContext';
import SalePaymentForm from '../components/SalePaymentForm'; // Import the form

function SaleManagement() {
    const { crops, customers, refreshData } = useData();
    const [sales, setSales] = useState([]);
    const [error, setError] = useState(null);

    // State for the payment modal
    const [showPaymentForm, setShowPaymentForm] = useState(false);
    const [payingSale, setPayingSale] = useState(null);

    const [formState, setFormState] = useState({
        crop_id: '',
        customer_id: '',
        sale_date: new Date().toISOString().slice(0, 10),
        quantity_sold_kg: '',
        selling_unit_price: ''
    });

    useEffect(() => {
        fetchSales();
    }, []);

    const fetchSales = async () => {
        try {
            const salesData = await getSales();
            setSales(salesData);
        } catch (error) {
            console.error("Failed to fetch sales:", error);
            setError("Failed to load sales.");
        }
    };

    const handleInputChange = (event) => {
        const { name, value } = event.target;
        setFormState(prevState => ({ ...prevState, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const submissionData = {
                ...formState,
                crop_id: parseInt(formState.crop_id),
                customer_id: parseInt(formState.customer_id),
                quantity_sold_kg: parseFloat(formState.quantity_sold_kg),
                selling_unit_price: parseFloat(formState.selling_unit_price)
            };
            await createSale(submissionData);
            fetchSales();
            setFormState({ crop_id: '', customer_id: '', sale_date: new Date().toISOString().slice(0, 10), quantity_sold_kg: '', selling_unit_price: '' });
        } catch (error) {
            console.error("Failed to create sale:", error);
            setError(error.response?.data?.detail || "An unexpected error occurred.");
        }
    };

    const handleRecordPayment = (sale) => {
        setPayingSale(sale);
        setShowPaymentForm(true);
    };

    const handleCancelPayment = () => {
        setShowPaymentForm(false);
        setPayingSale(null);
    };

    const handleSavePayment = async (paymentData) => {
        try {
            const response = await fetch('http://localhost:8000/api/v1/payments/', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(paymentData)
            });
            if (!response.ok) {
                throw new Error('Failed to save payment.');
            }
            handleCancelPayment();
            fetchSales();
        } catch (error) {
            console.error("Payment error:", error);
            setError(error.message);
        }
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case 'PAID': return <span className="badge bg-success">Paid</span>;
            case 'PARTIAL': return <span className="badge bg-warning text-dark">Partial</span>;
            case 'PENDING':
            default: return <span className="badge bg-danger">Pending</span>;
        }
    };

    return (
        <div className="container mt-4">
            {showPaymentForm && payingSale && 
                <SalePaymentForm 
                    sale={payingSale} 
                    onSave={handleSavePayment} 
                    onCancel={handleCancelPayment} 
                />
            }

            <h2>إدارة المبيعات</h2>
            {error && <div className="alert alert-danger">{error}</div>}

            {/* New Sale Form... */}
            <div className="card mb-4">
                 {/* ... form content from before ... */}
            </div>

            <div className="card">
                <div className="card-header">سجل المبيعات</div>
                <div className="card-body">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>التاريخ</th>
                                <th>العميل</th>
                                <th>المحصول</th>
                                <th>المبلغ الإجمالي</th>
                                <th>المستلم</th>
                                <th>الحالة</th>
                                <th>إجراءات</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sales.map(s => (
                                <tr key={s.sale_id}>
                                    <td>{s.sale_id}</td>
                                    <td>{s.sale_date}</td>
                                    <td>{s.customer.name}</td>
                                    <td>{s.crop.crop_name}</td>
                                    <td>{s.total_sale_amount.toFixed(2)}</td>
                                    <td>{(s.amount_received || 0).toFixed(2)}</td>
                                    <td>{getStatusBadge(s.payment_status)}</td>
                                    <td>
                                        <button 
                                            className="btn btn-sm btn-outline-success"
                                            onClick={() => handleRecordPayment(s)}
                                            disabled={s.payment_status === 'PAID'}
                                        >
                                            تسجيل دفعة
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default SaleManagement;
