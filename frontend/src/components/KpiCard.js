import React from 'react';

const KpiCard = ({ title, value, formatAsCurrency = true }) => {
    const formattedValue = formatAsCurrency 
        ? new Intl.NumberFormat('ar-EG', { style: 'currency', currency: 'EGP' }).format(value)
        : value;

    return (
        <div className="col-md-3 mb-4">
            <div className="card text-center">
                <div className="card-header">{title}</div>
                <div className="card-body">
                    <h4 className="card-title">{formattedValue}</h4>
                </div>
            </div>
        </div>
    );
};

export default KpiCard;
