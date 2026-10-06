package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WarehouseRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
  }

  fun observeItems(userId: String): Flow<List<InventoryItem>> {
    val path = "users/$userId/items"
    return db.collection("users").document(userId).collection("items")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMovements(userId: String): Flow<List<StockMovement>> {
    val path = "users/$userId/movements"
    return db.collection("users").document(userId).collection("movements")
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun getUserItems(userId: String = requireUserId()): Result<List<InventoryItem>> {
    val path = "users/$userId/items"
    return try {
      val snapshot = db.collection("users").document(userId).collection("items").get().await()
      val items = snapshot.documents.mapNotNull { doc ->
        doc.toObject(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun getItemById(targetUserId: String, itemId: String): Result<InventoryItem> {
    val path = "users/$targetUserId/items/$itemId"
    return try {
      val doc = db.collection("users").document(targetUserId).collection("items").document(itemId).get().await()
      val item = doc.toObject(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: throw NoSuchElementException("Barang tidak ditemukan")
      Result.success(item)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun createItem(item: InventoryItem): Result<String> {
    val uid = requireUserId()
    val itemsCol = db.collection("users").document(uid).collection("items")
    val docRef = if (item.id.isNotBlank()) itemsCol.document(item.id) else itemsCol.document()
    val path = docRef.path
    return try {
      val batch = db.batch()
      batch.set(docRef, item.toCreateMap(uid))

      // If initial stock > 0, also record an initial IN movement
      if (item.quantity > 0L) {
        val movRef = db.collection("users").document(uid).collection("movements").document()
        val initialMovement = StockMovement(
          userId = uid,
          itemId = docRef.id,
          itemName = item.name,
          itemSku = item.sku,
          type = MovementType.IN.code,
          quantityChanged = item.quantity,
          previousStock = 0L,
          newStock = item.quantity,
          referenceDoc = "INIT-${item.sku.take(12)}",
          partyName = item.supplierName.ifBlank { "Stok Awal Gudang" },
          notes = "Registrasi stok awal barang baru"
        )
        batch.set(movRef, initialMovement.toCreateMap(uid))
      }

      batch.commit().await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateItemDetails(item: InventoryItem): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("items").document(item.id)
    return try {
      docRef.update(item.toUpdateMap()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteItem(itemId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("items").document(itemId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordStockMovement(
    item: InventoryItem,
    movementType: MovementType,
    inputQuantity: Long,
    referenceDoc: String,
    partyName: String,
    notes: String
  ): Result<String> {
    val uid = requireUserId()
    val previousStock = item.quantity
    val newStock = when (movementType) {
      MovementType.IN -> previousStock + inputQuantity
      MovementType.OUT -> {
        if (inputQuantity > previousStock) {
          return Result.failure(IllegalArgumentException("Jumlah barang keluar ($inputQuantity) melebihi stok tersedia ($previousStock ${item.unit})."))
        }
        previousStock - inputQuantity
      }
      MovementType.ADJUST -> {
        if (inputQuantity < 0L) {
          return Result.failure(IllegalArgumentException("Stok fisik tidak boleh negatif."))
        }
        inputQuantity
      }
    }

    val quantityChanged = when (movementType) {
      MovementType.IN -> inputQuantity
      MovementType.OUT -> inputQuantity
      MovementType.ADJUST -> kotlin.math.abs(newStock - previousStock)
    }

    val itemRef = db.collection("users").document(uid).collection("items").document(item.id)
    val movRef = db.collection("users").document(uid).collection("movements").document()

    return try {
      val batch = db.batch()
      batch.update(
        itemRef,
        mapOf(
          "quantity" to newStock,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      val movement = StockMovement(
        userId = uid,
        itemId = item.id,
        itemName = item.name,
        itemSku = item.sku,
        type = movementType.code,
        quantityChanged = quantityChanged,
        previousStock = previousStock,
        newStock = newStock,
        referenceDoc = referenceDoc.ifBlank {
          when (movementType) {
            MovementType.IN -> "GRN-${System.currentTimeMillis() % 100000}"
            MovementType.OUT -> "DO-${System.currentTimeMillis() % 100000}"
            MovementType.ADJUST -> "OPN-${System.currentTimeMillis() % 100000}"
          }
        },
        partyName = partyName.ifBlank {
          when (movementType) {
            MovementType.IN -> item.supplierName.ifBlank { "Pemasok Umum" }
            MovementType.OUT -> "Distribusi Gudang"
            MovementType.ADJUST -> "Tim Audit Opname"
          }
        },
        notes = notes
      )
      batch.set(movRef, movement.toCreateMap(uid))
      batch.commit().await()
      Result.success(movRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, movRef.path)
      Result.failure(e)
    }
  }

  suspend fun seedSampleWarehouseData(): Result<Int> {
    val uid = requireUserId()
    val sampleItems = listOf(
      InventoryItem(
        sku = "ELK-204",
        name = "Scanner Barcode Industrial 2D",
        category = "Elektronik",
        locationRack = "Zona A - Rak 01",
        quantity = 18L,
        minStock = 5L,
        unit = "Unit",
        unitPrice = 1450000L,
        supplierName = "PT Solusi Otomasi Nusantara",
        notes = "Termasuk dudukan charger dan kabel USB industriales"
      ),
      InventoryItem(
        sku = "KMS-108",
        name = "Karton Box Double Wall 60x40x40",
        category = "Kemasan",
        locationRack = "Zona D - Fast Moving",
        quantity = 6L,
        minStock = 25L,
        unit = "Pcs",
        unitPrice = 18500L,
        supplierName = "CV Kemindo Jaya",
        notes = "Stok menipis, segera jadwalkan repeat order"
      ),
      InventoryItem(
        sku = "SKC-512",
        name = "Bearing Heavy Duty SKF 6205",
        category = "Suku Cadang",
        locationRack = "Zona B - Rak 02",
        quantity = 45L,
        minStock = 15L,
        unit = "Pcs",
        unitPrice = 85000L,
        supplierName = "PT Baja Makmur Teknik",
        notes = "Untuk konveyor lini produksi B"
      ),
      InventoryItem(
        sku = "LOG-019",
        name = "Stretch Film Wrapping 50cm x 300m",
        category = "Logistik & Palet",
        locationRack = "Area Staging / Loading",
        quantity = 0L,
        minStock = 10L,
        unit = "Roll",
        unitPrice = 92000L,
        supplierName = "CV Plastik Prima",
        notes = "Habis terpakai untuk pengiriman ekspedisi pagi"
      ),
      InventoryItem(
        sku = "ALT-301",
        name = "Hand Pallet Hydraulic 2.5 Ton",
        category = "Alat Kerja",
        locationRack = "Zona C - Heavy Duty",
        quantity = 4L,
        minStock = 2L,
        unit = "Unit",
        unitPrice = 3850000L,
        supplierName = "PT Indotara Persada",
        notes = "Kondisi prima, inspeksi berkala tiap bulan"
      )
    )

    return try {
      val batch = db.batch()
      sampleItems.forEach { sample ->
        val itemRef = db.collection("users").document(uid).collection("items").document()
        batch.set(itemRef, sample.toCreateMap(uid))

        val movRef = db.collection("users").document(uid).collection("movements").document()
        val movement = StockMovement(
          userId = uid,
          itemId = itemRef.id,
          itemName = sample.name,
          itemSku = sample.sku,
          type = MovementType.IN.code,
          quantityChanged = sample.quantity,
          previousStock = 0L,
          newStock = sample.quantity,
          referenceDoc = "PO-2026-${sample.sku.takeLast(3)}",
          partyName = sample.supplierName,
          notes = "Penerimaan stok perdana gudang"
        )
        batch.set(movRef, movement.toCreateMap(uid))
      }
      batch.commit().await()
      Result.success(sampleItems.size)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$uid/items")
      Result.failure(e)
    }
  }
}
