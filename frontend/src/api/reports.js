import axios from 'axios';

const API_URL = 'http://localhost:8000/api/v1/reports';

export const getGeneralLedger = async () => {
    try {
        const response = await axios.get(`${API_URL}/general-ledger`);
        return response.data;
    } catch (error) {
        console.error("Error fetching general ledger:", error);
        throw error;
    }
};

export const getTrialBalance = async () => {
    try {
        const response = await axios.get(`${API_URL}/trial-balance`);
        return response.data;
    } catch (error) {
        console.error("Error fetching trial balance:", error);
        throw error;
    }
};

export const getDashboardKpis = async () => {
    try {
        const response = await axios.get(`${API_URL}/dashboard-kpis`);
        return response.data;
    } catch (error) {
        console.error("Error fetching dashboard kpis:", error);
        throw error;
    }
};
