from sqlalchemy import Column, Integer, String, Boolean, Text, Float, Date, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class Crop(Base):
    __tablename__ = "crops"

    crop_id = Column(Integer, primary_key=True, index=True)
    crop_name = Column(String, unique=True, nullable=False, index=True)
    allowed_pricing_units = Column(Text, nullable=False)
    conversion_factors = Column(Text, nullable=False)
    is_active = Column(Boolean, default=True)

class Contact(Base):
    __tablename__ = "contacts"

    contact_id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False, index=True)
    phone = Column(String, nullable=True)
    address = Column(String, nullable=True)
    email = Column(String, nullable=True)
    is_supplier = Column(Boolean, default=False)
    is_customer = Column(Boolean, default=False)

class FinancialAccount(Base):
    __tablename__ = "financial_accounts"

    account_id = Column(Integer, primary_key=True, index=True)
    account_name = Column(String, unique=True, nullable=False)
    account_type = Column(String, nullable=False)
    current_balance = Column(Float, default=0.0)
    is_active = Column(Boolean, default=True)

class Purchase(Base):
    __tablename__ = "purchases"

    purchase_id = Column(Integer, primary_key=True, index=True)
    crop_id = Column(Integer, ForeignKey("crops.crop_id"), nullable=False)
    supplier_id = Column(Integer, ForeignKey("contacts.contact_id"), nullable=False)
    purchase_date = Column(Date, nullable=False)
    quantity_kg = Column(Float, nullable=False)
    unit_price = Column(Float, nullable=False)
    total_cost = Column(Float, nullable=False)
    amount_paid = Column(Float, default=0.0)
    payment_status = Column(String, default='PENDING')

    crop = relationship("Crop")
    supplier = relationship("Contact")

class Inventory(Base):
    __tablename__ = "inventory"

    inventory_id = Column(Integer, primary_key=True, index=True)
    crop_id = Column(Integer, ForeignKey("crops.crop_id"), nullable=False, unique=True)
    current_stock_kg = Column(Float, default=0.0)
    average_cost_per_kg = Column(Float, default=0.0)

    crop = relationship("Crop")

class Sale(Base):
    __tablename__ = "sales"

    sale_id = Column(Integer, primary_key=True, index=True)
    crop_id = Column(Integer, ForeignKey("crops.crop_id"), nullable=False)
    customer_id = Column(Integer, ForeignKey("contacts.contact_id"), nullable=False)
    sale_date = Column(Date, nullable=False)
    quantity_sold_kg = Column(Float, nullable=False)
    selling_unit_price = Column(Float, nullable=False)
    total_sale_amount = Column(Float, nullable=False)
    amount_received = Column(Float, default=0.0)
    payment_status = Column(String, default='PENDING')

    crop = relationship("Crop")
    customer = relationship("Contact")

class GeneralLedger(Base):
    __tablename__ = "general_ledger"

    entry_id = Column(Integer, primary_key=True, index=True)
    entry_date = Column(Date, nullable=False)
    account_id = Column(Integer, ForeignKey("financial_accounts.account_id"), nullable=False)
    debit = Column(Float, default=0.0)
    credit = Column(Float, default=0.0)
    description = Column(String)
    source_type = Column(String) # e.g., 'PURCHASE', 'SALE'
    source_id = Column(Integer) # e.g., purchase_id, sale_id

    account = relationship("FinancialAccount")

class Expense(Base):
    __tablename__ = "expenses"

    expense_id = Column(Integer, primary_key=True, index=True)
    expense_date = Column(Date, nullable=False)
    description = Column(String, nullable=False)
    amount = Column(Float, nullable=False)
    
    # The account that was credited (e.g., Cash, Bank)
    credit_account_id = Column(Integer, ForeignKey("financial_accounts.account_id"), nullable=False)
    # The expense account that was debited (e.g., Rent Expense, Fuel Expense)
    debit_account_id = Column(Integer, ForeignKey("financial_accounts.account_id"), nullable=False)
    
    supplier_id = Column(Integer, ForeignKey("contacts.contact_id"), nullable=True)

    credit_account = relationship("FinancialAccount", foreign_keys=[credit_account_id])
    debit_account = relationship("FinancialAccount", foreign_keys=[debit_account_id])
    supplier = relationship("Contact")

class Payment(Base):
    __tablename__ = "payments"

    payment_id = Column(Integer, primary_key=True, index=True)
    payment_date = Column(Date, nullable=False)
    amount = Column(Float, nullable=False)
    contact_id = Column(Integer, ForeignKey("contacts.contact_id"), nullable=False)
    payment_method = Column(String, nullable=False) # e.g., Cash, Bank Transfer

    # The account that was credited (e.g., Accounts Receivable)
    credit_account_id = Column(Integer, ForeignKey("financial_accounts.account_id"), nullable=False)
    # The account that was debited (e.g., Cash, Bank)
    debit_account_id = Column(Integer, ForeignKey("financial_accounts.account_id"), nullable=False)

    # Polymorphic relationship to link to either a Sale or a Purchase
    transaction_type = Column(String) # 'SALE' or 'PURCHASE'
    transaction_id = Column(Integer)

    contact = relationship("Contact")
    credit_account = relationship("FinancialAccount", foreign_keys=[credit_account_id])
    debit_account = relationship("FinancialAccount", foreign_keys=[debit_account_id])
