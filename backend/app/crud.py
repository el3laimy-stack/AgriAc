from sqlalchemy.orm import Session, joinedload
import json
from . import models, schemas

# --- Crop CRUD Functions ---

def get_crop(db: Session, crop_id: int):
    return db.query(models.Crop).filter(models.Crop.crop_id == crop_id).first()

def get_crops(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.Crop).offset(skip).limit(limit).all()

# --- Contact CRUD Functions ---

def get_contact(db: Session, contact_id: int):
    return db.query(models.Contact).filter(models.Contact.contact_id == contact_id).first()

def get_contacts(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.Contact).offset(skip).limit(limit).all()

# --- Financial Account CRUD Functions ---

def get_financial_account(db: Session, account_id: int):
    return db.query(models.FinancialAccount).filter(models.FinancialAccount.account_id == account_id).first()

def get_financial_accounts(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.FinancialAccount).order_by(models.FinancialAccount.account_name).offset(skip).limit(limit).all()

def create_financial_account(db: Session, account: schemas.FinancialAccountCreate) -> models.FinancialAccount:
    db_account = models.FinancialAccount(**account.model_dump())
    db.add(db_account)
    db.commit()
    db.refresh(db_account)
    return db_account

def update_financial_account(db: Session, account_id: int, account_update: schemas.FinancialAccountUpdate) -> models.FinancialAccount:
    db_account = get_financial_account(db, account_id)
    if db_account:
        update_data = account_update.model_dump(exclude_unset=True)
        for key, value in update_data.items():
            setattr(db_account, key, value)
        db.commit()
        db.refresh(db_account)
    return db_account

def delete_financial_account(db: Session, account_id: int) -> models.FinancialAccount:
    db_account = get_financial_account(db, account_id)
    if db_account:
        db_account.is_active = False
        db.commit()
        db.refresh(db_account)
    return db_account

def update_account_balance(db: Session, account_id: int, amount: float):
    account = db.query(models.FinancialAccount).filter(models.FinancialAccount.account_id == account_id).first()
    if account:
        account.current_balance += amount
        db.add(account)

# --- General Ledger CRUD Functions ---

def create_ledger_entry(db: Session, entry: schemas.GeneralLedgerCreate, source_type: str, source_id: int):
    db_entry = models.GeneralLedger(
        **entry.model_dump(),
        source_type=source_type,
        source_id=source_id
    )
    db.add(db_entry)
    return db_entry

# --- Inventory CRUD Functions ---

def get_or_create_inventory(db: Session, crop_id: int) -> models.Inventory:
    inventory = db.query(models.Inventory).filter(models.Inventory.crop_id == crop_id).first()
    if not inventory:
        inventory = models.Inventory(crop_id=crop_id)
        db.add(inventory)
        db.flush() # Use flush to get the object in the session without committing
    return inventory

def get_inventory_levels(db: Session):
    return db.query(models.Inventory).options(joinedload(models.Inventory.crop)).all()

# --- Purchase CRUD Functions ---

def get_purchases(db: Session, skip: int = 0, limit: int = 100):
    return (
        db.query(models.Purchase)
        .options(joinedload(models.Purchase.crop), joinedload(models.Purchase.supplier))
        .order_by(models.Purchase.purchase_date.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

def create_purchase_record(db: Session, purchase_data: dict) -> models.Purchase:
    db_purchase = models.Purchase(**purchase_data)
    db.add(db_purchase)
    return db_purchase


# --- Sale CRUD Functions ---

def get_sales(db: Session, skip: int = 0, limit: int = 100):
    return (
        db.query(models.Sale)
        .options(joinedload(models.Sale.crop), joinedload(models.Sale.customer))
        .order_by(models.Sale.sale_date.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

def create_sale_record(db: Session, sale_data: dict) -> models.Sale:
    db_sale = models.Sale(**sale_data)
    db.add(db_sale)
    return db_sale

# --- Expense CRUD Functions ---

def get_expenses(db: Session, skip: int = 0, limit: int = 100):
    return (
        db.query(models.Expense)
        .options(
            joinedload(models.Expense.credit_account),
            joinedload(models.Expense.debit_account),
            joinedload(models.Expense.supplier)
        )
        .order_by(models.Expense.expense_date.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

def create_expense(db: Session, expense: schemas.ExpenseCreate) -> models.Expense:
    # 1. Create the expense record
    db_expense = models.Expense(**expense.model_dump())
    db.add(db_expense)
    db.flush() # Flush to get the expense_id for the ledger entries

    # 2. Create General Ledger entries for the double-entry bookkeeping
    # Debit the expense account
    debit_entry = models.GeneralLedger(
        entry_date=db_expense.expense_date,
        account_id=db_expense.debit_account_id,
        debit=db_expense.amount,
        credit=0,
        description=f"Expense: {db_expense.description}",
        source_type='EXPENSE',
        source_id=db_expense.expense_id
    )
    db.add(debit_entry)

    # Credit the cash/bank account
    credit_entry = models.GeneralLedger(
        entry_date=db_expense.expense_date,
        account_id=db_expense.credit_account_id,
        debit=0,
        credit=db_expense.amount,
        description=f"Expense: {db_expense.description}",
        source_type='EXPENSE',
        source_id=db_expense.expense_id
    )
    db.add(credit_entry)

    # 3. Update account balances
    update_account_balance(db, account_id=db_expense.debit_account_id, amount=db_expense.amount) # Balance increases for debit expense accounts
    update_account_balance(db, account_id=db_expense.credit_account_id, amount=-db_expense.amount) # Balance decreases for credit asset accounts

    db.commit()
    db.refresh(db_expense)
    return db_expense
