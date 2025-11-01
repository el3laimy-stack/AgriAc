package accounting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Contact Model Test")
class ContactTest {

    @Test
    @DisplayName("Test Contact Getters and Setters")
    void testContactProperties() {
        // 1. Setup
        Contact contact = new Contact();

        // 2. Act
        contact.setContactId(1);
        contact.setName("مورد وعميل");
        contact.setPhone("123-456-7890");
        contact.setAddress("123 شارع الموردين");
        contact.setSupplier(true);
        contact.setCustomer(true);

        // 3. Assert
        assertEquals(1, contact.getContactId());
        assertEquals("مورد وعميل", contact.getName());
        assertEquals("123-456-7890", contact.getPhone());
        assertEquals("123 شارع الموردين", contact.getAddress());
        assertTrue(contact.isSupplier());
        assertTrue(contact.isCustomer());
        assertEquals("مورد وعميل", contact.getContactType());
        assertEquals("مورد وعميل", contact.toString());
    }

    @Test
    @DisplayName("Test Contact Constructor")
    void testContactConstructor() {
        // 1. Setup & 2. Act
        Contact contact = new Contact(2, "عميل فقط", "098-765-4321", "456 شارع العملاء", false, true);

        // 3. Assert
        assertEquals(2, contact.getContactId());
        assertEquals("عميل فقط", contact.getName());
        assertEquals("098-765-4321", contact.getPhone());
        assertEquals("456 شارع العملاء", contact.getAddress());
        assertFalse(contact.isSupplier());
        assertTrue(contact.isCustomer());
        assertEquals("عميل", contact.getContactType());
    }
}
