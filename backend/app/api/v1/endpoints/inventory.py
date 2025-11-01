from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.api.v1.endpoints.crops import get_db

router = APIRouter()

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List
import json

from app import crud, schemas
from app.api.v1.endpoints.crops import get_db

router = APIRouter()

class InventoryRead(schemas.BaseModel):
    current_stock_kg: float
    average_cost_per_kg: float
    crop: schemas.Crop
    model_config = schemas.ConfigDict(from_attributes=True)

@router.get("/", response_model=List[InventoryRead])
def read_inventory_levels(db: Session = Depends(get_db)):
    inventory_levels = crud.get_inventory_levels(db)
    
    response_inventory = []
    for inv in inventory_levels:
        response_crop = schemas.Crop(
            crop_id=inv.crop.crop_id,
            crop_name=inv.crop.crop_name,
            is_active=inv.crop.is_active,
            allowed_pricing_units=json.loads(inv.crop.allowed_pricing_units),
            conversion_factors=json.loads(inv.crop.conversion_factors)
        )
        
        inv_response = InventoryRead(
            current_stock_kg=inv.current_stock_kg,
            average_cost_per_kg=inv.average_cost_per_kg,
            crop=response_crop
        )
        response_inventory.append(inv_response)
        
    return response_inventory
