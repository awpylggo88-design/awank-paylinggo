package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOutWarehouseUser
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.data.model.WAREHOUSE_CATEGORIES
import com.example.data.repository.WarehouseRepository
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.InventoryItemCard
import com.example.ui.components.ItemFormDialog
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StockMovementDialog
import com.example.ui.components.StockMovementRowCard
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.InboundEmerald
import com.example.ui.theme.LowStockOrange
import com.example.ui.theme.OutboundBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class WarehouseTab(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String
) {
  DASHBOARD("Ringkasan", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
  INVENTORY("Stok Barang", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_inventory"),
  MOVEMENTS("Mutasi Stok", Icons.Filled.SwapVert, Icons.Outlined.SwapVert, "tab_movements"),
  ANALYTICS("Laporan Rak", Icons.Filled.Analytics, Icons.Outlined.Analytics, "tab_analytics")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseAppScreen(
  currentUserId: String,
  onSignedOut: () -> Unit
) {
  val viewModel: WarehouseViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        WarehouseViewModel(WarehouseRepository(db), currentUserId)
      }
    }
  )

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var currentTab by remember { mutableStateOf(WarehouseTab.DASHBOARD) }

  // Handle back press on non-home tabs
  BackHandler(enabled = currentTab != WarehouseTab.DASHBOARD) {
    currentTab = WarehouseTab.DASHBOARD
  }

  val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
  val filteredItemsState by viewModel.filteredItemsState.collectAsStateWithLifecycle()
  val movementsState by viewModel.movementsState.collectAsStateWithLifecycle()
  val filteredMovementsState by viewModel.filteredMovementsState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val selectedStockFilter by viewModel.selectedStockFilter.collectAsStateWithLifecycle()
  val selectedMovementFilter by viewModel.selectedMovementFilter.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  // Modal Dialog States
  var showItemForm by remember { mutableStateOf(false) }
  var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
  var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

  var showMovementDialog by remember { mutableStateOf(false) }
  var movementPreselectedItem by remember { mutableStateOf<InventoryItem?>(null) }
  var movementInitialType by remember { mutableStateOf(MovementType.IN) }

  LaunchedEffect(actionMessage) {
    actionMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  val allItems = (itemsState as? UiState.Success)?.data.orEmpty()
  val allMovements = (movementsState as? UiState.Success)?.data.orEmpty()

  val userEmail = Firebase.auth.currentUser?.email ?: "Operator Gudang"

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "GudangPintar",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = userEmail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              signOutWarehouseUser(
                context = context,
                onSignOutComplete = onSignedOut,
                scope = scope
              )
            },
            modifier = Modifier.testTag("logout_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Keluar Akun"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        windowInsets = WindowInsets.navigationBars
      ) {
        WarehouseTab.entries.forEach { tab ->
          val selected = currentTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title
              )
            },
            label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.testTag(tab.tag)
          )
        }
      }
    },
    floatingActionButton = {
      when (currentTab) {
        WarehouseTab.DASHBOARD, WarehouseTab.INVENTORY -> {
          ExtendedFloatingActionButton(
            onClick = {
              itemToEdit = null
              showItemForm = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Barang Baru") },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.testTag("fab_add_item")
          )
        }
        WarehouseTab.MOVEMENTS -> {
          if (allItems.isNotEmpty()) {
            ExtendedFloatingActionButton(
              onClick = {
                movementPreselectedItem = allItems.firstOrNull()
                movementInitialType = MovementType.IN
                showMovementDialog = true
              },
              icon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
              text = { Text("Catat Mutasi") },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_add_movement")
            )
          }
        }
        WarehouseTab.ANALYTICS -> {}
      }
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 840.dp)
      ) {
        AnimatedVisibility(visible = isBusy) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        when (currentTab) {
          WarehouseTab.DASHBOARD -> {
            DashboardTabContent(
              itemsState = itemsState,
              movements = allMovements,
              onAddNewItem = {
                itemToEdit = null
                showItemForm = true
              },
              onOpenMovementDialog = { type, item ->
                movementInitialType = type
                movementPreselectedItem = item ?: allItems.firstOrNull()
                showMovementDialog = true
              },
              onNavigateToLowStock = {
                viewModel.selectStockFilter(StockFilter.LOW_STOCK)
                currentTab = WarehouseTab.INVENTORY
              },
              onSeedSampleData = { viewModel.seedSampleData() }
            )
          }

          WarehouseTab.INVENTORY -> {
            InventoryTabContent(
              filteredItemsState = filteredItemsState,
              searchQuery = searchQuery,
              selectedCategory = selectedCategory,
              selectedStockFilter = selectedStockFilter,
              onSearchChange = viewModel::updateSearchQuery,
              onCategorySelect = viewModel::selectCategory,
              onStockFilterSelect = viewModel::selectStockFilter,
              onQuickMovement = { item, type ->
                movementPreselectedItem = item
                movementInitialType = type
                showMovementDialog = true
              },
              onEditItem = { item ->
                itemToEdit = item
                showItemForm = true
              },
              onDeleteItem = { item ->
                itemToDelete = item
              },
              onSeedSampleData = { viewModel.seedSampleData() }
            )
          }

          WarehouseTab.MOVEMENTS -> {
            MovementsTabContent(
              filteredMovementsState = filteredMovementsState,
              searchQuery = searchQuery,
              selectedType = selectedMovementFilter,
              hasItems = allItems.isNotEmpty(),
              onSearchChange = viewModel::updateSearchQuery,
              onSelectType = viewModel::selectMovementFilter,
              onOpenMovementModal = { type ->
                movementInitialType = type
                movementPreselectedItem = allItems.firstOrNull()
                showMovementDialog = true
              }
            )
          }

          WarehouseTab.ANALYTICS -> {
            AnalyticsTabContent(
              items = allItems,
              movements = allMovements
            )
          }
        }
      }
    }
  }

  if (showItemForm) {
    ItemFormDialog(
      initialItem = itemToEdit,
      onDismiss = {
        showItemForm = false
        itemToEdit = null
      },
      onSave = { savedItem ->
        val isEdit = itemToEdit != null
        viewModel.saveItem(savedItem, isEdit = isEdit) {
          showItemForm = false
          itemToEdit = null
        }
      }
    )
  }

  if (showMovementDialog && allItems.isNotEmpty()) {
    StockMovementDialog(
      items = allItems,
      preselectedItem = movementPreselectedItem,
      initialType = movementInitialType,
      onDismiss = {
        showMovementDialog = false
        movementPreselectedItem = null
      },
      onConfirm = { item, type, qty, refDoc, party, notes ->
        viewModel.recordMovement(
          item = item,
          movementType = type,
          quantity = qty,
          referenceDoc = refDoc,
          partyName = party,
          notes = notes
        ) {
          showMovementDialog = false
          movementPreselectedItem = null
        }
      }
    )
  }

  itemToDelete?.let { target ->
    ConfirmDeleteDialog(
      item = target,
      onDismiss = { itemToDelete = null },
      onConfirmDelete = {
        viewModel.deleteItem(target) {
          itemToDelete = null
        }
      }
    )
  }
}

@Composable
private fun DashboardTabContent(
  itemsState: UiState<List<InventoryItem>>,
  movements: List<StockMovement>,
  onAddNewItem: () -> Unit,
  onOpenMovementDialog: (MovementType, InventoryItem?) -> Unit,
  onNavigateToLowStock: () -> Unit,
  onSeedSampleData: () -> Unit
) {
  when (itemsState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = itemsState.message,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center
        )
      }
    }
    is UiState.Success -> {
      val items = itemsState.data
      val totalSku = items.size
      val totalUnits = items.sumOf { it.quantity }
      val totalAssetValue = items.sumOf { it.totalValue }
      val lowOrOutItems = items.filter { it.isLowStock || it.isOutOfStock }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Hero Warehouse Banner
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .height(165.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              Image(
                painter = painterResource(id = R.drawable.img_warehouse_hero_1791281966137),
                contentDescription = "Banner Gudang",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(
                        Color(0xFF0F172A).copy(alpha = 0.92f),
                        Color(0xFF0F172A).copy(alpha = 0.45f)
                      )
                    )
                  )
              )
              Column(
                modifier = Modifier
                  .align(Alignment.CenterStart)
                  .padding(20.dp)
              ) {
                Surface(
                  color = Color(0xFFD97706),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "LIVE CLOUD INVENTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Nilai Total Aset Gudang",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                  text = formatRupiah(totalAssetValue),
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "$totalSku SKU terdaftar • $totalUnits total unit fisik",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFFDE68A)
                )
              }
            }
          }
        }

        // Quick Action Buttons Row
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            QuickOperationButton(
              label = "Barang Masuk",
              icon = Icons.Default.ArrowDownward,
              color = InboundEmerald,
              enabled = items.isNotEmpty(),
              onClick = { onOpenMovementDialog(MovementType.IN, null) },
              modifier = Modifier
                .weight(1f)
                .testTag("quick_action_in")
            )
            QuickOperationButton(
              label = "Barang Keluar",
              icon = Icons.Default.ArrowUpward,
              color = OutboundBlue,
              enabled = items.isNotEmpty(),
              onClick = { onOpenMovementDialog(MovementType.OUT, null) },
              modifier = Modifier
                .weight(1f)
                .testTag("quick_action_out")
            )
            QuickOperationButton(
              label = "Stok Opname",
              icon = Icons.Default.Tune,
              color = LowStockOrange,
              enabled = items.isNotEmpty(),
              onClick = { onOpenMovementDialog(MovementType.ADJUST, null) },
              modifier = Modifier
                .weight(1f)
                .testTag("quick_action_opname")
            )
          }
        }

        // KPI Metric Grid (2x2)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              StatMetricCard(
                title = "Total Katalog SKU",
                value = "$totalSku Barang",
                subtitle = "${items.map { it.category }.distinct().size} Kategori Aktif",
                icon = Icons.Default.Inventory2,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
              )
              StatMetricCard(
                title = "Total Stok Fisik",
                value = "$totalUnits Unit",
                subtitle = "${items.map { it.locationRack }.distinct().size} Titik Rak",
                icon = Icons.Default.LocationOn,
                accentColor = InboundEmerald,
                modifier = Modifier.weight(1f)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              StatMetricCard(
                title = "Perlu Restock",
                value = "${lowOrOutItems.size} SKU",
                subtitle = "${items.count { it.isOutOfStock }} Habis • ${items.count { it.isLowStock }} Menipis",
                icon = Icons.Default.WarningAmber,
                accentColor = if (lowOrOutItems.isNotEmpty()) CriticalCrimson else InboundEmerald,
                modifier = Modifier.weight(1f),
                onClick = if (lowOrOutItems.isNotEmpty()) onNavigateToLowStock else null
              )
              StatMetricCard(
                title = "Aktivitas Mutasi",
                value = "${movements.size} Log",
                subtitle = "${movements.count { it.type == "IN" }} Masuk • ${movements.count { it.type == "OUT" }} Keluar",
                icon = Icons.Default.SwapVert,
                accentColor = OutboundBlue,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Empty Warehouse State with Sample Data Seeder
        if (items.isEmpty()) {
          item {
            EmptyWarehouseCard(
              onAddNewItem = onAddNewItem,
              onSeedSampleData = onSeedSampleData
            )
          }
        } else {
          // Low Stock Alert Section
          if (lowOrOutItems.isNotEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                  containerColor = CriticalCrimson.copy(alpha = 0.09f)
                )
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = CriticalCrimson
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "Peringatan Stok Kritis (${lowOrOutItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = CriticalCrimson,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                  lowOrOutItems.take(4).forEach { alertItem ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "${alertItem.name} (${alertItem.sku})",
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.SemiBold
                        )
                        Text(
                          text = "${alertItem.locationRack} • Sisa: ${alertItem.quantity} ${alertItem.unit} (Min: ${alertItem.minStock})",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Button(
                        onClick = { onOpenMovementDialog(MovementType.IN, alertItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = InboundEmerald),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text("+ Restock", style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
          }

          // Recent Stock Movements Section
          item {
            Text(
              text = "Mutasi Barang Terbaru",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
          }

          if (movements.isEmpty()) {
            item {
              Text(
                text = "Belum ada riwayat mutasi barang keluar/masuk.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            items(movements.take(6), key = { it.id }) { mov ->
              StockMovementRowCard(movement = mov)
            }
          }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
      }
    }
  }
}

@Composable
private fun QuickOperationButton(
  label: String,
  icon: ImageVector,
  color: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(52.dp),
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = color.copy(alpha = 0.15f),
      contentColor = color
    ),
    contentPadding = PaddingValues(horizontal = 8.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun EmptyWarehouseCard(
  onAddNewItem: () -> Unit,
  onSeedSampleData: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Inventory2,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(32.dp)
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Gudang Anda Masih Kosong",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Tambahkan barang pertama Anda secara manual atau muat katalog contoh gudang siap pakai untuk mencoba fitur Barang Masuk, Keluar, dan Stok Opname.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(18.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onSeedSampleData,
          modifier = Modifier.testTag("seed_sample_data_button")
        ) {
          Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Isi Data Contoh")
        }
        Button(
          onClick = onAddNewItem,
          modifier = Modifier.testTag("empty_add_item_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tambah Barang")
        }
      }
    }
  }
}

@Composable
private fun InventoryTabContent(
  filteredItemsState: UiState<List<InventoryItem>>,
  searchQuery: String,
  selectedCategory: String?,
  selectedStockFilter: StockFilter,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (String?) -> Unit,
  onStockFilterSelect: (StockFilter) -> Unit,
  onQuickMovement: (InventoryItem, MovementType) -> Unit,
  onEditItem: (InventoryItem) -> Unit,
  onDeleteItem: (InventoryItem) -> Unit,
  onSeedSampleData: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text("Cari nama barang, SKU, rak, atau pemasok...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_inventory_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Stock Status Filter Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StockFilter.entries.forEach { filter ->
        FilterChip(
          selected = selectedStockFilter == filter,
          onClick = { onStockFilterSelect(filter) },
          label = { Text(filter.label) },
          modifier = Modifier.testTag("stock_filter_${filter.name.lowercase()}")
        )
      }
    }

    // Category Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedCategory == null,
        onClick = { onCategorySelect(null) },
        label = { Text("Semua Kategori") }
      )
      WAREHOUSE_CATEGORIES.forEach { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = {
            onCategorySelect(if (selectedCategory == cat) null else cat)
          },
          label = { Text(cat) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (filteredItemsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(filteredItemsState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        val items = filteredItemsState.data
        if (items.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Tidak ada barang yang cocok dengan filter pencarian.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              if (searchQuery.isBlank() && selectedCategory == null && selectedStockFilter == StockFilter.ALL) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onSeedSampleData) {
                  Icon(Icons.Default.CloudDownload, contentDescription = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Muat 5 Barang Contoh")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(items, key = { it.id }) { item ->
              InventoryItemCard(
                item = item,
                onQuickIn = { onQuickMovement(item, MovementType.IN) },
                onQuickOut = { onQuickMovement(item, MovementType.OUT) },
                onAdjustStock = { onQuickMovement(item, MovementType.ADJUST) },
                onEditItem = { onEditItem(item) },
                onDeleteItem = { onDeleteItem(item) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MovementsTabContent(
  filteredMovementsState: UiState<List<StockMovement>>,
  searchQuery: String,
  selectedType: MovementType?,
  hasItems: Boolean,
  onSearchChange: (String) -> Unit,
  onSelectType: (MovementType?) -> Unit,
  onOpenMovementModal: (MovementType) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text("Cari No. PO/SJ, nama barang, SKU, atau pihak...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari mutasi") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_movements_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedType == null,
        onClick = { onSelectType(null) },
        label = { Text("Semua Mutasi") }
      )
      MovementType.entries.forEach { type ->
        FilterChip(
          selected = selectedType == type,
          onClick = { onSelectType(if (selectedType == type) null else type) },
          label = { Text(type.label) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (filteredMovementsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(filteredMovementsState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        val list = filteredMovementsState.data
        if (list.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Belum ada catatan mutasi stok yang sesuai.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              if (hasItems) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { onOpenMovementModal(MovementType.IN) }) {
                  Text("Catat Transaksi Pertama")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(list, key = { it.id }) { mov ->
              StockMovementRowCard(movement = mov)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AnalyticsTabContent(
  items: List<InventoryItem>,
  movements: List<StockMovement>
) {
  val totalValue = items.sumOf { it.totalValue }.coerceAtLeast(1L)
  val byCategory = items.groupBy { it.category }
    .mapValues { (_, list) ->
      Triple(list.size, list.sumOf { it.quantity }, list.sumOf { it.totalValue })
    }
    .toList()
    .sortedByDescending { it.second.third }

  val byRack = items.groupBy { it.locationRack }
    .mapValues { (_, list) ->
      Pair(list.size, list.sumOf { it.quantity })
    }
    .toList()
    .sortedByDescending { it.second.second }

  val totalInQty = movements.filter { it.type == "IN" }.sumOf { it.quantityChanged }
  val totalOutQty = movements.filter { it.type == "OUT" }.sumOf { it.quantityChanged }
  val totalAdjustCount = movements.count { it.type == "ADJUST" }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Summary Flow Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Ringkasan Arus Logistik Gudang",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Total Masuk (IN)", style = MaterialTheme.typography.bodySmall)
              Text(
                text = "+$totalInQty Unit",
                style = MaterialTheme.typography.titleLarge,
                color = InboundEmerald,
                fontWeight = FontWeight.Bold
              )
            }
            Column {
              Text("Total Keluar (OUT)", style = MaterialTheme.typography.bodySmall)
              Text(
                text = "-$totalOutQty Unit",
                style = MaterialTheme.typography.titleLarge,
                color = OutboundBlue,
                fontWeight = FontWeight.Bold
              )
            }
            Column {
              Text("Audit Opname", style = MaterialTheme.typography.bodySmall)
              Text(
                text = "$totalAdjustCount Kali",
                style = MaterialTheme.typography.titleLarge,
                color = LowStockOrange,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Valuasi per Kategori
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Category,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Distribusi Nilai & Stok per Kategori",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    }

    if (byCategory.isEmpty()) {
      item {
        Text(
          text = "Belum ada data barang untuk dianalisis.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      items(byCategory, key = { it.first }) { (categoryName, stats) ->
        val (skuCount, unitSum, catValue) = stats
        val ratio = (catValue.toFloat() / totalValue.toFloat()).coerceIn(0f, 1f)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = formatRupiah(catValue),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$skuCount SKU • $unitSum total unit (${(ratio * 100).toInt()}% dari nilai gudang)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { ratio },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = MaterialTheme.colorScheme.secondary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }
    }

    // Kapasitas & Kepadatan Lokasi Rak
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pemetaan Lokasi Zona & Rak Gudang",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    }

    items(byRack, key = { it.first }) { (rackName, rackStats) ->
      val (skuCount, totalUnitsInRack) = rackStats
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = rackName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Menyimpan $skuCount jenis SKU barang",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "$totalUnitsInRack Unit",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
  }
}
