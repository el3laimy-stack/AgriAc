import React, { useState } from 'react';
import { createContact } from '../api/contacts';
import { useData } from '../context/DataContext'; // Import the custom hook

function ContactManagement() {
    // Consume contacts and the refresh function from the global context
    const { contacts, refreshData } = useData();
    
    const [formState, setFormState] = useState({
        name: '',
        phone: '',
        address: '',
        email: '',
        is_supplier: false,
        is_customer: false
    });

    // No longer need local fetchContacts or useEffect

    const handleInputChange = (event) => {
        const { name, value, type, checked } = event.target;
        setFormState(prevState => ({
            ...prevState,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await createContact(formState);
            refreshData(); // Trigger a global data refresh
            setFormState({
                name: '', phone: '', address: '', email: '',
                is_supplier: false, is_customer: false
            });
        } catch (error) {
            console.error("Failed to create contact:", error);
        }
    };

    return (
        <div className="container mt-4">
            <h2>إدارة جهات التعامل</h2>
            
            <div className="card mb-4">
                <div className="card-header">إضافة جهة تعامل جديدة</div>
                <div className="card-body">
                    <form onSubmit={handleSubmit}>
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <label htmlFor="name" className="form-label">الاسم</label>
                                <input type="text" className="form-control" id="name" name="name" value={formState.name} onChange={handleInputChange} required />
                            </div>
                            <div className="col-md-6 mb-3">
                                <label htmlFor="phone" className="form-label">الهاتف</label>
                                <input type="text" className="form-control" id="phone" name="phone" value={formState.phone} onChange={handleInputChange} />
                            </div>
                        </div>
                        <div className="row">
                             <div className="col-md-6 mb-3">
                                <label htmlFor="address" className="form-label">العنوان</label>
                                <input type="text" className="form-control" id="address" name="address" value={formState.address} onChange={handleInputChange} />
                            </div>
                            <div className="col-md-6 mb-3">
                                <label htmlFor="email" className="form-label">البريد الإلكتروني</label>
                                <input type="email" className="form-control" id="email" name="email" value={formState.email} onChange={handleInputChange} />
                            </div>
                        </div>
                        <div className="row">
                            <div className="col-md-6 mb-3">
                                <div className="form-check">
                                    <input className="form-check-input" type="checkbox" id="is_customer" name="is_customer" checked={formState.is_customer} onChange={handleInputChange} />
                                    <label className="form-check-label" htmlFor="is_customer">عميل</label>
                                </div>
                                <div className="form-check">
                                    <input className="form-check-input" type="checkbox" id="is_supplier" name="is_supplier" checked={formState.is_supplier} onChange={handleInputChange} />
                                    <label className="form-check-label" htmlFor="is_supplier">مورد</label>
                                </div>
                            </div>
                        </div>
                        <button type="submit" className="btn btn-primary">حفظ</button>
                    </form>
                </div>
            </div>

            <div className="card">
                <div className="card-header">قائمة جهات التعامل</div>
                <div className="card-body">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>الاسم</th>
                                <th>الهاتف</th>
                                <th>النوع</th>
                            </tr>
                        </thead>
                        <tbody>
                            {contacts.map(contact => (
                                <tr key={contact.contact_id}>
                                    <td>{contact.contact_id}</td>
                                    <td>{contact.name}</td>
                                    <td>{contact.phone}</td>
                                    <td>
                                        {contact.is_customer && <span className="badge bg-success me-1">عميل</span>}
                                        {contact.is_supplier && <span className="badge bg-info">مورد</span>}
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

export default ContactManagement;
