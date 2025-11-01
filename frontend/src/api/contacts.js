import axios from 'axios';

// تعريف عنوان URL الأساسي للـ API لتجنب تكراره
const API_URL = 'http://localhost:8000/api/v1/contacts';

/**
 * دالة لجلب قائمة جهات التعامل من الخادم
 * @returns {Promise<Array>} قائمة بجهات التعامل
 */
export const getContacts = async () => {
    try {
        const response = await axios.get(API_URL);
        return response.data;
    } catch (error) {
        // تسجيل الخطأ في الكونسول لتسهيل تصحيح الأخطاء
        console.error("Error fetching contacts:", error);
        // إلقاء الخطأ مرة أخرى للسماح للمكون الذي استدعى الدالة بمعالجته
        throw error;
    }
};

/**
 * دالة لإنشاء جهة تعامل جديدة في الخادم
 * @param {object} contactData - بيانات جهة التعامل الجديدة
 * @returns {Promise<object>} جهة التعامل التي تم إنشاؤها
 */
export const createContact = async (contactData) => {
    try {
        const response = await axios.post(API_URL, contactData);
        return response.data;
    } catch (error) {
        console.error("Error creating contact:", error);
        throw error;
    }
};
