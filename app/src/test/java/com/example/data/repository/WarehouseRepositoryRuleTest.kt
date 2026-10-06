package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WarehouseRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveItem_authenticatedOwner_succeedsAndLogsInitialMovement() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = WarehouseRepository(firestore)
    val uniqueId = "item_${UUID.randomUUID().toString().replace("-", "")}"

    val sampleItem = InventoryItem(
      id = uniqueId,
      sku = "SKU-TEST-01",
      name = "Pallet Plastik Heavy Duty",
      category = "Logistik & Palet",
      locationRack = "Zona A - Rak 01",
      quantity = 20L,
      minStock = 5L,
      unit = "Pcs",
      unitPrice = 250000L,
      supplierName = "PT Palet Nusantara",
      notes = "Uji coba gudang"
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createItem(sampleItem) }
    assertTrue("Expected item creation to succeed", createResult.isSuccess)
    assertEquals(uniqueId, createResult.getOrThrow())

    val emittedItems = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeItems(aliceUid).first { list -> list.any { it.id == uniqueId } }
    }
    val fetched = emittedItems.first { it.id == uniqueId }
    assertEquals("Pallet Plastik Heavy Duty", fetched.name)
    assertEquals(20L, fetched.quantity)

    // Record an outbound stock movement
    val outResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordStockMovement(
        item = fetched,
        movementType = MovementType.OUT,
        inputQuantity = 5L,
        referenceDoc = "SJ-001",
        partyName = "Cabang Bandung",
        notes = "Pengiriman reguler"
      )
    }
    assertTrue("Expected outbound stock movement to succeed", outResult.isSuccess)

    val updatedItem = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getItemById(aliceUid, uniqueId).getOrThrow()
    }
    assertEquals(15L, updatedItem.quantity)
  }

  @Test
  fun getItemById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = WarehouseRepository(firestore)
    val uniqueId = "item_${UUID.randomUUID().toString().replace("-", "")}"

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createItem(
        InventoryItem(
          id = uniqueId,
          sku = "SKU-SECRET",
          name = "Barang Rahasia Alice",
          category = "Elektronik",
          locationRack = "Zona A - Rak 01",
          quantity = 10L,
          minStock = 2L,
          unit = "Unit",
          unitPrice = 500000L
        )
      )
    }
    assertTrue(createResult.isSuccess)

    signInTestUser(BOB_EMAIL)
    val bobRepo = WarehouseRepository(firestore)
    val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getItemById(aliceUid, uniqueId)
    }
    assertTrue("Expected cross-user read to fail", crossReadResult.isFailure)
    val exception = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }

  @Test
  fun observeItems_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = WarehouseRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeItems(aliceUid).first()
      }
      fail("Expected permission denied exception for unauthenticated user")
    } catch (e: Throwable) {
      var current: Throwable? = e
      var firestoreEx: FirebaseFirestoreException? = null
      while (current != null) {
        if (current is FirebaseFirestoreException) {
          firestoreEx = current
          break
        }
        current = current.cause
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_warehouse@test.com"
    const val BOB_EMAIL = "bob_warehouse@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
