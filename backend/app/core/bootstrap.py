from sqlalchemy.orm import Session
from app import models, crud

# تعريف أرقام الحسابات الأساسية كمتغيرات ثابتة لسهولة الوصول إليها
INVENTORY_ACCOUNT_ID = 10103
ACCOUNTS_PAYABLE_ID = 20101
CASH_ACCOUNT_ID = 10101
SALES_REVENUE_ACCOUNT_ID = 40101
COGS_ACCOUNT_ID = 50101
ACCOUNTS_RECEIVABLE_ID = 10104

def bootstrap_financial_accounts(db: Session):
    """
    Checks for and creates the default financial accounts if they don't exist.
    """
    accounts_to_create = [
        {'account_id': INVENTORY_ACCOUNT_ID, 'account_name': 'المخزون', 'account_type': 'CURRENT_ASSET'},
        {'account_id': ACCOUNTS_PAYABLE_ID, 'account_name': 'الذمم الدائنة (الموردين)', 'account_type': 'ACCOUNTS_PAYABLE'},
        {'account_id': CASH_ACCOUNT_ID, 'account_name': 'الخزنة الرئيسية', 'account_type': 'CASH'},
        {'account_id': 40101, 'account_name': 'إيرادات المبيعات', 'account_type': 'REVENUE'},
        {'account_id': 50101, 'account_name': 'تكلفة البضاعة المباعة', 'account_type': 'EXPENSE'},
        {'account_id': 10104, 'account_name': 'الذمم المدينة (العملاء)', 'account_type': 'ACCOUNTS_RECEIVABLE'},
    ]

    for acc_data in accounts_to_create:
        acc = crud.get_financial_account(db, account_id=acc_data['account_id'])
        if not acc:
            crud.create_financial_account(db, models.FinancialAccount(**acc_data))
