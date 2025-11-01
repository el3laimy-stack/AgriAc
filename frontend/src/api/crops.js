import axios from 'axios';

const API_URL = 'http://localhost:8000/api/v1';

export const getCrops = async () => {
    try {
        const response = await axios.get(`${API_URL}/crops/`);
        return response.data;
    } catch (error) {
        console.error("Error fetching crops:", error);
        throw error;
    }
};

export const createCrop = async (cropData) => {
    try {
        const response = await axios.post(`${API_URL}/crops/`, cropData);
        return response.data;
    } catch (error) {
        console.error("Error creating crop:", error);
        throw error;
    }
};
