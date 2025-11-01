import React, { useState, useEffect } from 'react';
import { getPurchases, createPurchase } from '../api/purchases';
import { useData } from '../context/DataContext'; // Import the custom hook
import PaymentForm from '../components/PaymentForm'; // Import the PaymentForm

function PurchaseManagement() {
    // Consume shared data from the context
    const { crops, suppliers, refreshData } = useData();

    // State for local data (purchases list)
    const [purchases, setPurchases] = useState([]);
    const [error, setError] = useState(null);

    // State for the payment modal
    const [showPaymentForm, setShowPaymentForm] = useState(false);
    const [payingPurchase, setPayingPurchase] = useState(null);

    // State for the new purchase form
    const [formState, setFormState] = useState({
        crop_id: '',
        supplier_id: '',
        purchase_date: new Date().toISOString().slice(0, 10),
        quantity_kg: '',
        unit_price: ''
    });

    useEffect(() => {
        fetchPurchases();
    }, []);

    const fetchPurchases = async () => {
        try {
            const purchasesData = await getPurchases();
            setPurchases(purchasesData);
        } catch (error) {
            console.error("Failed to fetch purchases:", error);
            setError("Failed to load purchases.");
        }
    };

    const handleInputChange = (event) => {
        const { name, value } = event.target;
        setFormState(prevState => ({
            ...prevState,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const submissionData = {
                ...formState,
                crop_id: parseInt(formState.crop_id),
                supplier_id: parseInt(formState.supplier_id),
                quantity_kg: parseFloat(formState.quantity_kg),
                unit_price: parseFloat(formState.unit_price)
            };
            await createPurchase(submissionData);
            fetchPurchases();
            setFormState({ crop_id: '', supplier_id: '', purchase_date: new Date().toISOString().slice(0, 10), quantity_kg: '', unit_price: '' });
        } catch (error) {
            console.error("Failed to create purchase:", error);
            setError("Failed to create purchase.");
        }
    };

    const handleRecordPayment = (purchase) => {
        setPayingPurchase(purchase);
        setShowPaymentForm(true);
    };

    const handleCancelPayment = () => {
        setShowPaymentForm(false);
        setPayingPurchase(null);
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
            fetchPurchases(); // Refresh the list
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
            {showPaymentForm && payingPurchase && 
                <PaymentForm 
                    purchase={payingPurchase} 
                    onSave={handleSavePayment} 
                    onCancel={handleCancelPayment} 
                />
            }

            <h2>إدارة المشتريات</h2>
            {error && <div className="alert alert-danger">{error}</div>}

            {/* New Purchase Form... */}
            <div className="card mb-4">
                {/* ... form content from before ... */}
            </div>

            <div className="card">
                <div className="card-header">سجل المشتريات</div>
                <div className="card-body">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>التاريخ</th>
                                <th>المورد</th>
                                <th>المحصول</th>
                                <th>التكلفة الإجمالية</th>
                                <th>المدفوع</th>
                                <th>الحالة</th>
                                <th>إجراءات</th>
                            </tr>
                        </thead>
                        <tbody>
                            {purchases.map(p => (
                                <tr key={p.purchase_id}>
                                    <td>{p.purchase_id}</td>
                                    <td>{p.purchase_date}</td>
                                    <td>{p.supplier.name}</td>
                                    <td>{p.crop.crop_name}</td>
                                    <td>{p.total_cost.toFixed(2)}</td>
                                    <td>{p.amount_paid.toFixed(2)}</td>
                                    <td>{getStatusBadge(p.payment_status)}</td>
                                    <td>
                                        <button 
                                            className="btn btn-sm btn-outline-success"
                                            onClick={() => handleRecordPayment(p)}
                                            disabled={p.payment_status === 'PAID'}
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

export default PurchaseManagement;
