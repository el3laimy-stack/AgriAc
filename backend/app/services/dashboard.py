from sqlalchemy.orm import Session
from sqlalchemy import func
from app import models

def get_dashboard_kpis(db: Session):
    total_revenue = db.query(func.sum(models.Sale.total_sale_amount)).scalar() or 0
    total_cogs = db.query(func.sum(models.GeneralLedger.debit)).filter(models.GeneralLedger.account_id == 50101).scalar() or 0
    inventory_value = db.query(func.sum(models.Inventory.current_stock_kg * models.Inventory.average_cost_per_kg)).scalar() or 0
    
    return {
        "total_revenue": total_revenue,
        "gross_profit": total_revenue - total_cogs,
        "inventory_value": inventory_value,
        "sales_count": db.query(func.count(models.Sale.sale_id)).scalar() or 0
    }
