#pragma once
#include <ctime>

struct {
    uint32_t ShipmentID;
    uint32_t InvetoryID;
    double WholsalePrice;
    int32_t Quantity;
    time_t ExpirationDate;
} menu_item;

struct {
    uint32_t ShipmentID;
    uint32_t InvetoryID;
    double WholsalePrice;
    uint32_t Quantity;
    time_t ExpirationDate;
} inventory_item;

struct {
    uint32_t MenuItemID;
    uint32_t InvetoryID;
    uint32_t QuantityRequired;
} menu_item_inventory;

struct {
    uint32_t OrderID;
    time_t OrderDate;
    uint32_t OrderTime;
    double Total;
} order;

struct {
    uint32_t OrderItemID;
    uint32_t OrderID;
    uint32_t MenuItemID;
    uint32_t Qty;
} order_items;
