package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue

data class InventoryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val sku: String = "",
  val name: String = "",
  val category: String = "Umum",
  val locationRack: String = "Zona A - Rak 01",
  val quantity: Long = 0L,
  val minStock: Long = 5L,
  val unit: String = "Pcs",
  val unitPrice: Long = 0L,
  val supplierName: String = "",
  val notes: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val isOutOfStock: Boolean
    get() = quantity <= 0L

  val isLowStock: Boolean
    get() = quantity in 1L..minStock

  val totalValue: Long
    get() = quantity * unitPrice

  fun toCreateMap(ownerUid: String): Map<String, Any> {
    return mapOf(
      "userId" to ownerUid,
      "sku" to sku.trim(),
      "name" to name.trim(),
      "category" to category.trim(),
      "locationRack" to locationRack.trim(),
      "quantity" to quantity,
      "minStock" to minStock,
      "unit" to unit.trim(),
      "unitPrice" to unitPrice,
      "supplierName" to supplierName.trim(),
      "notes" to notes.trim(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
  }

  fun toUpdateMap(): Map<String, Any> {
    return mapOf(
      "sku" to sku.trim(),
      "name" to name.trim(),
      "category" to category.trim(),
      "locationRack" to locationRack.trim(),
      "quantity" to quantity,
      "minStock" to minStock,
      "unit" to unit.trim(),
      "unitPrice" to unitPrice,
      "supplierName" to supplierName.trim(),
      "notes" to notes.trim(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
  }
}

enum class MovementType(val code: String, val label: String) {
  IN("IN", "Barang Masuk"),
  OUT("OUT", "Barang Keluar"),
  ADJUST("ADJUST", "Stok Opname");

  companion object {
    fun fromCode(code: String): MovementType {
      return entries.find { it.code == code } ?: IN
    }
  }
}

data class StockMovement(
  @DocumentId val id: String = "",
  val userId: String = "",
  val itemId: String = "",
  val itemName: String = "",
  val itemSku: String = "",
  val type: String = "IN",
  val quantityChanged: Long = 0L,
  val previousStock: Long = 0L,
  val newStock: Long = 0L,
  val referenceDoc: String = "",
  val partyName: String = "",
  val notes: String = "",
  val createdAt: Timestamp? = null
) {
  val movementType: MovementType
    get() = MovementType.fromCode(type)

  fun toCreateMap(ownerUid: String): Map<String, Any> {
    return mapOf(
      "userId" to ownerUid,
      "itemId" to itemId,
      "itemName" to itemName.trim(),
      "itemSku" to itemSku.trim(),
      "type" to type,
      "quantityChanged" to quantityChanged,
      "previousStock" to previousStock,
      "newStock" to newStock,
      "referenceDoc" to referenceDoc.trim(),
      "partyName" to partyName.trim(),
      "notes" to notes.trim(),
      "createdAt" to FieldValue.serverTimestamp()
    )
  }
}

val WAREHOUSE_CATEGORIES = listOf(
  "Elektronik",
  "Bahan Baku",
  "Suku Cadang",
  "Kemasan",
  "Alat Kerja",
  "Barang Jadi",
  "Logistik & Palet"
)

val WAREHOUSE_UNITS = listOf(
  "Pcs",
  "Box",
  "Karton",
  "Unit",
  "Kg",
  "Liter",
  "Roll",
  "Pallet",
  "Set"
)

val WAREHOUSE_RACKS = listOf(
  "Zona A - Rak 01",
  "Zona A - Rak 02",
  "Zona A - Rak 03",
  "Zona B - Rak 01",
  "Zona B - Rak 02",
  "Zona C - Heavy Duty",
  "Zona D - Fast Moving",
  "Area Staging / Loading"
)
