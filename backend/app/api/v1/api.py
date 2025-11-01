from fastapi import APIRouter

from app.api.v1.endpoints import crops, contacts, purchases, sales, inventory, reports, financial_accounts, expenses, journal, payments

api_router = APIRouter()
api_router.include_router(crops.router, prefix="/crops", tags=["Crops"])
api_router.include_router(contacts.router, prefix="/contacts", tags=["Contacts"])
api_router.include_router(purchases.router, prefix="/purchases", tags=["Purchases"])
api_router.include_router(sales.router, prefix="/sales", tags=["Sales"])
api_router.include_router(inventory.router, prefix="/inventory", tags=["Inventory"])
api_router.include_router(reports.router, prefix="/reports", tags=["Reports"])
api_router.include_router(financial_accounts.router, prefix="/financial-accounts", tags=["Financial Accounts"])
api_router.include_router(expenses.router, prefix="/expenses", tags=["Expenses"])
api_router.include_router(journal.router, prefix="/journal", tags=["Journal"])
api_router.include_router(payments.router, prefix="/payments", tags=["Payments"])
