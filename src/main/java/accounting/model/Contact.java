package accounting.model;

import java.util.Objects;

/**
 * نموذج بيانات جهة التعامل المحسن
 * يمثل مورداً أو عميلاً أو كليهما
 */
public class Contact {
    private int contactId;
    private String name;
    private String phone;
    private String address;
    private boolean isSupplier;
    private boolean isCustomer;

    // Constructors
    public Contact() {}

    public Contact(int contactId, String name, String phone, String address, 
                   boolean isSupplier, boolean isCustomer) {
        this.contactId = contactId;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.isSupplier = isSupplier;
        this.isCustomer = isCustomer;
    }

    // Getters and Setters
    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isSupplier() { return isSupplier; }
    public void setSupplier(boolean supplier) { isSupplier = supplier; }

    public boolean isCustomer() { return isCustomer; }
    public void setCustomer(boolean customer) { isCustomer = customer; }

    /**
     * يحصل على نوع جهة التعامل كنص
     * @return نص يوضح نوع جهة التعامل
     */
    public String getContactType() {
        if (isSupplier && isCustomer) {
            return "مورد وعميل";
        } else if (isSupplier) {
            return "مورد";
        } else if (isCustomer) {
            return "عميل";
        } else {
            return "غير محدد";
        }
    }

    @Override
    public String toString() {
        return name != null ? name : "جهة تعامل غير محددة";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Contact contact = (Contact) obj;
        return contactId == contact.contactId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactId);
    }
}

