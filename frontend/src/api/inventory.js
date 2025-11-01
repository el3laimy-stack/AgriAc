import axios from 'axios';

const API_URL = 'http://localhost:8000/api/v1/inventory';

export const getInventory = async () => {
    try {
        const response = await axios.get(API_URL);
        return response.data;
    } catch (error) {
        console.error("Error fetching inventory:", error);
        throw error;
    }
};
