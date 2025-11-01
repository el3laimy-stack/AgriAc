from sqlalchemy.orm import Session
from fastapi import HTTPException

from app import crud, schemas
from app.core.bootstrap import (
    INVENTORY_ACCOUNT_ID, 
    ACCOUNTS_RECEIVABLE_ID, 
    SALES_REVENUE_ACCOUNT_ID,
    COGS_ACCOUNT_ID
)

def create_new_sale(db: Session, sale: schemas.SaleCreate):
    """
    Creates a new sale record and handles all related business logic:
    1. Calculates total sale amount.
    2. Calculates Cost of Goods Sold (COGS).
    3. Updates inventory stock (decrease).
    4. Creates double-entry general ledger records.
    5. Updates financial account balances.
    All within a single database transaction.
    """
    # Validate crop and customer
    crop = crud.get_crop(db, sale.crop_id)
    if not crop:
        raise HTTPException(status_code=404, detail=f"Crop with id {sale.crop_id} not found.")
    
    customer = crud.get_contact(db, sale.customer_id)
    if not customer or not customer.is_customer:
        raise HTTPException(status_code=404, detail=f"Customer with id {sale.customer_id} not found.")

    inventory = crud.get_or_create_inventory(db, crop_id=sale.crop_id)
    if inventory.current_stock_kg < sale.quantity_sold_kg:
        raise HTTPException(status_code=400, detail=f"Not enough stock for crop '{crop.crop_name}'. Available: {inventory.current_stock_kg}kg")

    total_sale_amount = sale.quantity_sold_kg * sale.selling_unit_price
    cost_of_goods_sold = inventory.average_cost_per_kg * sale.quantity_sold_kg

    try:
        # 1. Update Inventory
        inventory.current_stock_kg -= sale.quantity_sold_kg
        db.add(inventory)

        # 2. Create the Sale record
        sale_data = sale.model_dump()
        sale_data['total_sale_amount'] = total_sale_amount
        db_sale = crud.create_sale_record(db, sale_data=sale_data)
        db.flush() # Get the sale_id for the ledger entries

        # 3. Create General Ledger entries
        sale_description = f"Sale of {sale.quantity_sold_kg}kg of {crop.crop_name} to {customer.name}"
        cogs_description = f"COGS for sale of {crop.crop_name} to {customer.name}"

        # Entry 1: Debit Accounts Receivable, Credit Sales Revenue
        crud.create_ledger_entry(db, schemas.GeneralLedgerCreate(entry_date=sale.sale_date, account_id=ACCOUNTS_RECEIVABLE_ID, debit=total_sale_amount, description=sale_description), 'SALE', db_sale.sale_id)
        crud.create_ledger_entry(db, schemas.GeneralLedgerCreate(entry_date=sale.sale_date, account_id=SALES_REVENUE_ACCOUNT_ID, credit=total_sale_amount, description=sale_description), 'SALE', db_sale.sale_id)
        
        # Entry 2: Debit COGS, Credit Inventory
        crud.create_ledger_entry(db, schemas.GeneralLedgerCreate(entry_date=sale.sale_date, account_id=COGS_ACCOUNT_ID, debit=cost_of_goods_sold, description=cogs_description), 'SALE', db_sale.sale_id)
        crud.create_ledger_entry(db, schemas.GeneralLedgerCreate(entry_date=sale.sale_date, account_id=INVENTORY_ACCOUNT_ID, credit=cost_of_goods_sold, description=cogs_description), 'SALE', db_sale.sale_id)

        # 4. Update account balances
        crud.update_account_balance(db, account_id=ACCOUNTS_RECEIVABLE_ID, amount=total_sale_amount)
        crud.update_account_balance(db, account_id=SALES_REVENUE_ACCOUNT_ID, amount=-total_sale_amount) # Revenue accounts have a credit balance
        crud.update_account_balance(db, account_id=COGS_ACCOUNT_ID, amount=cost_of_goods_sold)
        crud.update_account_balance(db, account_id=INVENTORY_ACCOUNT_ID, amount=-cost_of_goods_sold)

        db.commit()
        db.refresh(db_sale)
        return db_sale

    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"An error occurred during the sale transaction: {e}")
