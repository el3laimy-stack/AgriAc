import React, { useState, useEffect } from 'react';
import ExpenseForm from '../components/ExpenseForm';

const ExpenseManagement = () => {
    const [expenses, setExpenses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);

    const fetchExpenses = async () => {
        try {
            setLoading(true);
            const response = await fetch('http://localhost:8000/api/v1/expenses/');
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.json();
            setExpenses(data);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchExpenses();
    }, []);

    const handleSave = async (formData) => {
        // Ensure empty string for supplier_id is converted to null
        const payload = {
            ...formData,
            supplier_id: formData.supplier_id || null
        };

        try {
            const response = await fetch('http://localhost:8000/api/v1/expenses/', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                const errData = await response.json();
                throw new Error(errData.detail || 'Failed to create expense');
            }

            setShowForm(false);
            fetchExpenses(); // Refresh the list
        } catch (error) {
            setError(error.message);
        }
    };

    if (loading && !showForm) return <div>Loading...</div>;
    if (error) return <div className="alert alert-danger">Error: {error}</div>;

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h1>Expense Management</h1>
                <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ Add New Expense</button>
            </div>

            {showForm && (
                <div className="card card-body mb-4">
                    <h3>New Expense</h3>
                    <ExpenseForm 
                        onSave={handleSave} 
                        onCancel={() => setShowForm(false)} 
                    />
                </div>
            )}

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Date</th>
                        <th>Description</th>
                        <th>Amount</th>
                        <th>Debit Account</th>
                        <th>Credit Account</th>
                        <th>Supplier</th>
                    </tr>
                </thead>
                <tbody>
                    {expenses.map(expense => (
                        <tr key={expense.expense_id}>
                            <td>{expense.expense_id}</td>
                            <td>{new Date(expense.expense_date + 'T00:00:00').toLocaleDateString()}</td>
                            <td>{expense.description}</td>
                            <td>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(expense.amount)}</td>
                            <td>{expense.debit_account.account_name}</td>
                            <td>{expense.credit_account.account_name}</td>
                            <td>{expense.supplier ? expense.supplier.name : 'N/A'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default ExpenseManagement;
