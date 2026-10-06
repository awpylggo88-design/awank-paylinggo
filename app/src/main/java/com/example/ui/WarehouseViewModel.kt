package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class StockFilter(val label: String) {
  ALL("Semua"),
  LOW_STOCK("Stok Menipis"),
  OUT_OF_STOCK("Habis")
}

class WarehouseViewModel(
  private val repository: WarehouseRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow<String?>(null)
  val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

  private val _selectedStockFilter = MutableStateFlow(StockFilter.ALL)
  val selectedStockFilter: StateFlow<StockFilter> = _selectedStockFilter.asStateFlow()

  private val _selectedMovementFilter = MutableStateFlow<MovementType?>(null)
  val selectedMovementFilter: StateFlow<MovementType?> = _selectedMovementFilter.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  val itemsState: StateFlow<UiState<List<InventoryItem>>> = repository.observeItems(currentUserId)
    .map<List<InventoryItem>, UiState<List<InventoryItem>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing warehouse items", error)
      emit(UiState.Error(error.localizedMessage ?: "Gagal memuat daftar barang gudang"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val movementsState: StateFlow<UiState<List<StockMovement>>> = repository.observeMovements(currentUserId)
    .map<List<StockMovement>, UiState<List<StockMovement>>> { UiState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing stock movements", error)
      emit(UiState.Error(error.localizedMessage ?: "Gagal memuat riwayat mutasi stok"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val filteredItemsState: StateFlow<UiState<List<InventoryItem>>> = combine(
    itemsState,
    _searchQuery,
    _selectedCategory,
    _selectedStockFilter
  ) { state, query, category, stockFilter ->
    when (state) {
      is UiState.Loading -> UiState.Loading
      is UiState.Error -> state
      is UiState.Success -> {
        val q = query.trim().lowercase()
        val filtered = state.data.filter { item ->
          val matchesQuery = q.isEmpty() ||
            item.name.lowercase().contains(q) ||
            item.sku.lowercase().contains(q) ||
            item.locationRack.lowercase().contains(q) ||
            item.supplierName.lowercase().contains(q)

          val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)

          val matchesStock = when (stockFilter) {
            StockFilter.ALL -> true
            StockFilter.LOW_STOCK -> item.isLowStock || item.isOutOfStock
            StockFilter.OUT_OF_STOCK -> item.isOutOfStock
          }

          matchesQuery && matchesCategory && matchesStock
        }
        UiState.Success(filtered)
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = UiState.Loading
  )

  val filteredMovementsState: StateFlow<UiState<List<StockMovement>>> = combine(
    movementsState,
    _selectedMovementFilter,
    _searchQuery
  ) { state, movFilter, query ->
    when (state) {
      is UiState.Loading -> UiState.Loading
      is UiState.Error -> state
      is UiState.Success -> {
        val q = query.trim().lowercase()
        val filtered = state.data.filter { mov ->
          val matchesType = movFilter == null || mov.type == movFilter.code
          val matchesQuery = q.isEmpty() ||
            mov.itemName.lowercase().contains(q) ||
            mov.itemSku.lowercase().contains(q) ||
            mov.referenceDoc.lowercase().contains(q) ||
            mov.partyName.lowercase().contains(q)
          matchesType && matchesQuery
        }
        UiState.Success(filtered)
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = UiState.Loading
  )

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(category: String?) {
    _selectedCategory.value = category
  }

  fun selectStockFilter(filter: StockFilter) {
    _selectedStockFilter.value = filter
  }

  fun selectMovementFilter(type: MovementType?) {
    _selectedMovementFilter.value = type
  }

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun saveItem(item: InventoryItem, isEdit: Boolean, onSuccess: () -> Unit = {}) {
    if (item.sku.isBlank() || item.name.isBlank()) {
      _actionMessage.value = "Kode SKU dan Nama Barang wajib diisi."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val result = if (isEdit) {
        repository.updateItemDetails(item)
      } else {
        repository.createItem(item).map { Unit }
      }
      _isBusy.value = false
      result.fold(
        onSuccess = {
          _actionMessage.value = if (isEdit) {
            "Data barang '${item.name}' berhasil diperbarui."
          } else {
            "Barang '${item.name}' berhasil ditambahkan ke gudang."
          }
          onSuccess()
        },
        onFailure = { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menyimpan data barang."
        }
      )
    }
  }

  fun deleteItem(item: InventoryItem, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.deleteItem(item.id)
      _isBusy.value = false
      result.fold(
        onSuccess = {
          _actionMessage.value = "Barang '${item.name}' dihapus dari gudang."
          onSuccess()
        },
        onFailure = { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menghapus barang."
        }
      )
    }
  }

  fun recordMovement(
    item: InventoryItem,
    movementType: MovementType,
    quantity: Long,
    referenceDoc: String,
    partyName: String,
    notes: String,
    onSuccess: () -> Unit = {}
  ) {
    if (movementType != MovementType.ADJUST && quantity <= 0L) {
      _actionMessage.value = "Jumlah barang harus lebih dari 0."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.recordStockMovement(
        item = item,
        movementType = movementType,
        inputQuantity = quantity,
        referenceDoc = referenceDoc,
        partyName = partyName,
        notes = notes
      )
      _isBusy.value = false
      result.fold(
        onSuccess = {
          _actionMessage.value = "${movementType.label} untuk '${item.name}' berhasil dicatat."
          onSuccess()
        },
        onFailure = { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal mencatat mutasi stok."
        }
      )
    }
  }

  fun seedSampleData() {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.seedSampleWarehouseData()
      _isBusy.value = false
      result.fold(
        onSuccess = { count ->
          _actionMessage.value = "$count data barang contoh berhasil dimuat ke gudang Anda."
        },
        onFailure = { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal memuat data contoh."
        }
      )
    }
  }

  private companion object {
    const val TAG = "WarehouseVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
