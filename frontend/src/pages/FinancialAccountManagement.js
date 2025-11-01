import React, { useState, useEffect } from 'react';
import FinancialAccountForm from '../components/FinancialAccountForm';

const FinancialAccountManagement = () => {
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingAccount, setEditingAccount] = useState(null);
    const [showForm, setShowForm] = useState(false);

    const fetchAccounts = async () => {
        try {
            setLoading(true);
            const response = await fetch('http://localhost:8000/api/v1/financial-accounts/');
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.json();
            setAccounts(data);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAccounts();
    }, []);

    const handleSave = async (formData) => {
        const isEditing = !!formData.account_id;
        const url = isEditing 
            ? `http://localhost:8000/api/v1/financial-accounts/${formData.account_id}`
            : 'http://localhost:8000/api/v1/financial-accounts/';
        const method = isEditing ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            if (!response.ok) {
                throw new Error(`Failed to ${isEditing ? 'update' : 'create'} account`);
            }

            handleCancel();
            fetchAccounts(); // Refresh the list
        } catch (error) {
            setError(error.message);
        }
    };

    const handleEdit = (account) => {
        setEditingAccount(account);
        setShowForm(true);
    };

    const handleAddNew = () => {
        setEditingAccount(null);
        setShowForm(true);
    };

    const handleCancel = () => {
        setShowForm(false);
        setEditingAccount(null);
    };

    const handleDeactivate = async (accountId) => {
        if (window.confirm('Are you sure you want to deactivate this account?')) {
            try {
                const response = await fetch(`http://localhost:8000/api/v1/financial-accounts/${accountId}`, {
                    method: 'DELETE',
                });

                if (!response.ok) {
                    throw new Error('Failed to deactivate account');
                }

                fetchAccounts(); // Refresh the list
            } catch (error) {
                setError(error.message);
            }
        }
    };

    if (loading && !showForm) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h1>Financial Account Management</h1>
                <button className="btn btn-primary" onClick={handleAddNew}>+ Add New Account</button>
            </div>

            {showForm && (
                <div className="card card-body mb-4">
                    <h3>{editingAccount ? 'Edit Financial Account' : 'New Financial Account'}</h3>
                    <FinancialAccountForm 
                        account={editingAccount}
                        onSave={handleSave} 
                        onCancel={handleCancel} 
                    />
                </div>
            )}

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Account Name</th>
                        <th>Account Type</th>
                        <th>Balance</th>
                        <th>Active</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {accounts.map(account => (
                        <tr key={account.account_id}>
                            <td>{account.account_id}</td>
                            <td>{account.account_name}</td>
                            <td>{account.account_type}</td>
                            <td>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(account.current_balance)}</td>
                            <td>
                                <span className={`badge ${account.is_active ? 'bg-success' : 'bg-danger'}`}>
                                    {account.is_active ? 'Yes' : 'No'}
                                </span>
                            </td>
                            <td>
                                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => handleEdit(account)}>Edit</button>
                                <button className="btn btn-sm btn-outline-danger" onClick={() => handleDeactivate(account.account_id)}>Deactivate</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default FinancialAccountManagement;
