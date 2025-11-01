from pydantic import BaseModel, ConfigDict
from typing import Optional, Dict, List
from datetime import date

# --- Base Schemas ---
class CropBase(BaseModel):
    crop_name: str
    is_active: Optional[bool] = True

class ContactBase(BaseModel):
    name: str
    phone: Optional[str] = None
    address: Optional[str] = None
    email: Optional[str] = None
    is_supplier: Optional[bool] = False
    is_customer: Optional[bool] = False

# --- Crop Schemas ---
class CropCreate(CropBase):
    allowed_pricing_units: List[str]
    conversion_factors: Dict[str, float]

class Crop(CropBase):
    crop_id: int
    allowed_pricing_units: List[str]
    conversion_factors: Dict[str, float]
    model_config = ConfigDict(from_attributes=True)

# --- Contact Schemas ---
class ContactCreate(ContactBase):
    pass

class Contact(ContactBase):
    contact_id: int
    model_config = ConfigDict(from_attributes=True)

# --- Financial Account Schemas ---
class FinancialAccountBase(BaseModel):
    account_name: str
    account_type: str

class FinancialAccountCreate(FinancialAccountBase):
    current_balance: float = 0.0

class FinancialAccountUpdate(FinancialAccountBase):
    is_active: Optional[bool] = None

class FinancialAccount(FinancialAccountBase):
    account_id: int
    current_balance: float
    is_active: bool
    model_config = ConfigDict(from_attributes=True)

# --- General Ledger Schemas ---
class GeneralLedgerBase(BaseModel):
    entry_date: date
    account_id: int
    debit: float = 0.0
    credit: float = 0.0
    description: Optional[str] = None

class GeneralLedgerCreate(GeneralLedgerBase):
    pass

class GeneralLedger(GeneralLedgerBase):
    entry_id: int
    model_config = ConfigDict(from_attributes=True)

# --- Purchase Schemas ---
class PurchaseBase(BaseModel):
    crop_id: int
    supplier_id: int
    purchase_date: date
    quantity_kg: float
    unit_price: float

class PurchaseCreate(PurchaseBase):
    pass

class PurchaseRead(PurchaseBase):
    purchase_id: int
    total_cost: float
    crop: Crop
    supplier: Contact
    model_config = ConfigDict(from_attributes=True)


# --- Sale Schemas ---
class SaleBase(BaseModel):
    crop_id: int
    customer_id: int
    sale_date: date
    quantity_sold_kg: float
    selling_unit_price: float

class SaleCreate(SaleBase):
    pass

class SaleRead(SaleBase):
    sale_id: int
    total_sale_amount: float
    crop: Crop
    customer: Contact
    model_config = ConfigDict(from_attributes=True)

# --- Expense Schemas ---
class ExpenseBase(BaseModel):
    expense_date: date
    description: str
    amount: float
    credit_account_id: int
    debit_account_id: int
    supplier_id: Optional[int] = None

class ExpenseCreate(ExpenseBase):
    pass

class ExpenseRead(ExpenseBase):
    expense_id: int
    credit_account: FinancialAccount
    debit_account: FinancialAccount
    supplier: Optional[Contact] = None
    model_config = ConfigDict(from_attributes=True)

# --- Payment Schemas ---
class PaymentBase(BaseModel):
    payment_date: date
    amount: float
    contact_id: int
    payment_method: str
    credit_account_id: int
    debit_account_id: int
    transaction_type: str
    transaction_id: int

class PaymentCreate(PaymentBase):
    pass

class PaymentRead(PaymentBase):
    payment_id: int
    contact: Contact
    credit_account: FinancialAccount
    debit_account: FinancialAccount
    model_config = ConfigDict(from_attributes=True)
