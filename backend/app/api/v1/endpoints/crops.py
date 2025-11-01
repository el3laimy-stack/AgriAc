from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import json

from app import crud, models, schemas
from app.database import SessionLocal

router = APIRouter()

# Dependency to get a DB session
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/", response_model=schemas.Crop)
def create_crop(crop: schemas.CropCreate, db: Session = Depends(get_db)):
    db_crop = crud.get_crop_by_name(db, name=crop.crop_name)
    if db_crop:
        raise HTTPException(status_code=400, detail="Crop with this name already registered")
    return crud.create_crop(db=db, crop=crop)

@router.get("/", response_model=List[schemas.Crop])
def read_crops(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    db_crops = crud.get_crops(db, skip=skip, limit=limit)
    response_crops = []
    for db_crop in db_crops:
        response_crop = schemas.Crop(
            crop_id=db_crop.crop_id,
            crop_name=db_crop.crop_name,
            allowed_pricing_units=json.loads(db_crop.allowed_pricing_units),
            conversion_factors=json.loads(db_crop.conversion_factors),
            is_active=db_crop.is_active
        )
        response_crops.append(response_crop)
    return response_crops

@router.get("/{crop_id}", response_model=schemas.Crop)
def read_crop(crop_id: int, db: Session = Depends(get_db)):
    db_crop = crud.get_crop(db, crop_id=crop_id)
    if db_crop is None:
        raise HTTPException(status_code=404, detail="Crop not found")
    
    return schemas.Crop(
        crop_id=db_crop.crop_id,
        crop_name=db_crop.crop_name,
        allowed_pricing_units=json.loads(db_crop.allowed_pricing_units),
        conversion_factors=json.loads(db_crop.conversion_factors),
        is_active=db_crop.is_active
    )
