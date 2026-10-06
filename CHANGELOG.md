# Changelog

## Phase 2: Database Population

### Summary
While building the data generator, we found that the Phase 1 could not represent some inventory data. Ingredients are measured in fractions, shipments arrive on specific dates and items have different shelf lives.

**No tables were added or removed, and all relationships and cardinalities are unchanged.**

### Added
- **`INVENTORY.Unit`** (string, Not Null): gives each quantity a unit so stock levels are known.
- **`INVENTORY.ShelfLifeDays`** (int, Not Null): records how long each item lasts.
- **`INVENTORYITEM.ReceivedDate`** (date, Not Null): records when each shipment arrived for expiration date.

### Changed
- **`INVENTORYITEM.ShipmentID`**: int → bigint, to allow for a larger history.
- **`INVENTORYITEM.Quantity`**: int → decimal, since shipments arrive in fractions.
- **`INVENTORY.Minimum_Threshold`** → **`Minimum_Threshold`**: changed to decimal to match `Quantity`.
- **`INVENTORY.Quantity`**: int → decimal, since stock is fractional and comes from shipments.
- **`MENUITEMINVENTORY.QuantityRequired`**: int → decimal, since recipes use fractional amounts.
- **`ORDERITEM.Qty`**: default removed so quantity is always written.

### Unchanged
- `MENUITEM` (`MenuItemID`, `MenuItemName`, `Price`)
- `ORDERS` (`OrderID`, `OrderDate`, `OrderTime`, `Total`)
- All primary keys, foreign keys, and relationships:

---

## Phase 1: Database Design

### Added
- Initial schema with six tables: `INVENTORY`, `INVENTORYITEM`, `MENUITEM`, `MENUITEMINVENTORY`, `ORDERS`, `ORDERITEM`.
