package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.example.data.model.WAREHOUSE_CATEGORIES
import com.example.data.model.WAREHOUSE_RACKS
import com.example.data.model.WAREHOUSE_UNITS
import com.example.ui.theme.InboundEmerald
import com.example.ui.theme.LowStockOrange
import com.example.ui.theme.OutboundBlue

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemFormDialog(
  initialItem: InventoryItem? = null,
  onDismiss: () -> Unit,
  onSave: (InventoryItem) -> Unit
) {
  val isEdit = initialItem != null
  var sku by remember {
    mutableStateOf(initialItem?.sku ?: "BRG-${(100..999).random()}")
  }
  var name by remember { mutableStateOf(initialItem?.name ?: "") }
  var category by remember { mutableStateOf(initialItem?.category ?: WAREHOUSE_CATEGORIES.first()) }
  var locationRack by remember { mutableStateOf(initialItem?.locationRack ?: WAREHOUSE_RACKS.first()) }
  var quantityText by remember { mutableStateOf((initialItem?.quantity ?: 10L).toString()) }
  var minStockText by remember { mutableStateOf((initialItem?.minStock ?: 5L).toString()) }
  var unit by remember { mutableStateOf(initialItem?.unit ?: WAREHOUSE_UNITS.first()) }
  var unitPriceText by remember { mutableStateOf((initialItem?.unitPrice ?: 50000L).toString()) }
  var supplierName by remember { mutableStateOf(initialItem?.supplierName ?: "") }
  var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
  var formError by remember { mutableStateOf<String?>(null) }

  var rackExpanded by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text(
          text = if (isEdit) "Ubah Data Barang" else "Tambah Barang Baru",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Lengkapi identitas SKU, lokasi rak, dan batas minimum stok gudang.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = sku,
            onValueChange = { sku = it.uppercase().take(35) },
            label = { Text("Kode SKU*") },
            singleLine = true,
            modifier = Modifier
              .weight(0.45f)
              .testTag("input_item_sku")
          )
          OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(110) },
            label = { Text("Nama Barang*") },
            singleLine = true,
            modifier = Modifier
              .weight(0.55f)
              .testTag("input_item_name")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Kategori Barang",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          WAREHOUSE_CATEGORIES.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
          expanded = rackExpanded,
          onExpandedChange = { rackExpanded = !rackExpanded }
        ) {
          OutlinedTextField(
            value = locationRack,
            onValueChange = { locationRack = it.take(55) },
            label = { Text("Lokasi Zona & Rak Gudang*") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackExpanded) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryEditable)
              .testTag("input_item_rack")
          )
          ExposedDropdownMenu(
            expanded = rackExpanded,
            onDismissRequest = { rackExpanded = false }
          ) {
            WAREHOUSE_RACKS.forEach { rackOption ->
              DropdownMenuItem(
                text = { Text(rackOption) },
                onClick = {
                  locationRack = rackOption
                  rackExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
            label = { Text(if (isEdit) "Stok Saat Ini" else "Stok Awal*") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("input_item_quantity")
          )
          OutlinedTextField(
            value = minStockText,
            onValueChange = { minStockText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Min. Stok Alert*") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("input_item_min_stock")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Satuan Unit",
          style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          WAREHOUSE_UNITS.forEach { u ->
            FilterChip(
              selected = unit == u,
              onClick = { unit = u },
              label = { Text(u, style = MaterialTheme.typography.bodySmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = unitPriceText,
          onValueChange = { unitPriceText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Harga Satuan (Rp)*") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_item_price")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = supplierName,
          onValueChange = { supplierName = it.take(90) },
          label = { Text("Nama Pemasok / Vendor") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_item_supplier")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it.take(400) },
          label = { Text("Catatan Penyimpanan / Spesifikasi") },
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_item_notes")
        )

        if (formError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = formError!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("cancel_item_button")
          ) {
            Text("Batal")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              if (sku.isBlank() || name.isBlank() || locationRack.isBlank()) {
                formError = "SKU, Nama Barang, dan Lokasi Rak wajib diisi."
                return@Button
              }
              val qty = quantityText.toLongOrNull() ?: 0L
              val minQty = minStockText.toLongOrNull() ?: 0L
              val price = unitPriceText.toLongOrNull() ?: 0L
              onSave(
                InventoryItem(
                  id = initialItem?.id ?: "",
                  userId = initialItem?.userId ?: "",
                  sku = sku.trim(),
                  name = name.trim(),
                  category = category.trim(),
                  locationRack = locationRack.trim(),
                  quantity = qty,
                  minStock = minQty,
                  unit = unit.trim(),
                  unitPrice = price,
                  supplierName = supplierName.trim(),
                  notes = notes.trim(),
                  createdAt = initialItem?.createdAt,
                  updatedAt = initialItem?.updatedAt
                )
              )
            },
            modifier = Modifier.testTag("save_item_button")
          ) {
            Text(if (isEdit) "Simpan Perubahan" else "Simpan Barang")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementDialog(
  items: List<InventoryItem>,
  preselectedItem: InventoryItem? = null,
  initialType: MovementType = MovementType.IN,
  onDismiss: () -> Unit,
  onConfirm: (
    item: InventoryItem,
    type: MovementType,
    quantity: Long,
    referenceDoc: String,
    partyName: String,
    notes: String
  ) -> Unit
) {
  var selectedItem by remember { mutableStateOf(preselectedItem ?: items.firstOrNull()) }
  var movementType by remember { mutableStateOf(initialType) }
  var quantityText by remember {
    mutableStateOf(
      if (initialType == MovementType.ADJUST && preselectedItem != null) {
        preselectedItem.quantity.toString()
      } else {
        "1"
      }
    )
  }
  var referenceDoc by remember {
    mutableStateOf(
      when (initialType) {
        MovementType.IN -> "PO-2026-${(100..999).random()}"
        MovementType.OUT -> "SJ-2026-${(100..999).random()}"
        MovementType.ADJUST -> "OPN-2026-${(100..999).random()}"
      }
    )
  }
  var partyName by remember {
    mutableStateOf(
      if (initialType == MovementType.IN) selectedItem?.supplierName.orEmpty() else ""
    )
  }
  var notes by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var itemDropdownExpanded by remember { mutableStateOf(false) }

  val accentColor = when (movementType) {
    MovementType.IN -> InboundEmerald
    MovementType.OUT -> OutboundBlue
    MovementType.ADJUST -> LowStockOrange
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text(
          text = "Catat Mutasi Stok Gudang",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Pilih jenis transaksi dan barang untuk memperbarui stok secara otomatis.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Movement Type Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MovementType.entries.forEach { typeOption ->
            val isSelected = movementType == typeOption
            FilterChip(
              selected = isSelected,
              onClick = {
                movementType = typeOption
                referenceDoc = when (typeOption) {
                  MovementType.IN -> "PO-2026-${(100..999).random()}"
                  MovementType.OUT -> "SJ-2026-${(100..999).random()}"
                  MovementType.ADJUST -> "OPN-2026-${(100..999).random()}"
                }
                if (typeOption == MovementType.ADJUST && selectedItem != null) {
                  quantityText = selectedItem!!.quantity.toString()
                }
              },
              label = { Text(typeOption.label, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(
                  imageVector = when (typeOption) {
                    MovementType.IN -> Icons.Default.ArrowDownward
                    MovementType.OUT -> Icons.Default.ArrowUpward
                    MovementType.ADJUST -> Icons.Default.Tune
                  },
                  contentDescription = null
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor.copy(alpha = 0.18f),
                selectedLabelColor = accentColor,
                selectedLeadingIconColor = accentColor
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Item Selector Dropdown
        ExposedDropdownMenuBox(
          expanded = itemDropdownExpanded,
          onExpandedChange = { itemDropdownExpanded = !itemDropdownExpanded }
        ) {
          OutlinedTextField(
            value = selectedItem?.let { "${it.sku} - ${it.name} (Stok: ${it.quantity} ${it.unit})" }
              ?: "Pilih Barang",
            onValueChange = {},
            readOnly = true,
            label = { Text("Barang Gudang*") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = itemDropdownExpanded,
            onDismissRequest = { itemDropdownExpanded = false }
          ) {
            items.forEach { option ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text("${option.sku} • ${option.name}", fontWeight = FontWeight.SemiBold)
                    Text(
                      "Stok: ${option.quantity} ${option.unit} | ${option.locationRack}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                },
                onClick = {
                  selectedItem = option
                  if (movementType == MovementType.IN && partyName.isBlank()) {
                    partyName = option.supplierName
                  }
                  if (movementType == MovementType.ADJUST) {
                    quantityText = option.quantity.toString()
                  }
                  itemDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val qtyLabel = when (movementType) {
          MovementType.IN -> "Jumlah Barang Masuk (${selectedItem?.unit ?: "Unit"})*"
          MovementType.OUT -> "Jumlah Barang Keluar (Maks: ${selectedItem?.quantity ?: 0})*"
          MovementType.ADJUST -> "Hasil Hitung Stok Fisik Aktual (${selectedItem?.unit ?: "Unit"})*"
        }

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
          label = { Text(qtyLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_movement_qty")
        )

        selectedItem?.let { current ->
          val inputVal = quantityText.toLongOrNull() ?: 0L
          val projected = when (movementType) {
            MovementType.IN -> current.quantity + inputVal
            MovementType.OUT -> current.quantity - inputVal
            MovementType.ADJUST -> inputVal
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Estimasi Stok Akhir: ${current.quantity} → $projected ${current.unit}",
            style = MaterialTheme.typography.labelMedium,
            color = if (projected < 0) MaterialTheme.colorScheme.error else accentColor
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = referenceDoc,
          onValueChange = { referenceDoc = it.take(70) },
          label = {
            Text(
              when (movementType) {
                MovementType.IN -> "No. PO / Surat Terima Barang"
                MovementType.OUT -> "No. Surat Jalan / DO"
                MovementType.ADJUST -> "No. Berita Acara Opname"
              }
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_movement_ref")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = partyName,
          onValueChange = { partyName = it.take(90) },
          label = {
            Text(
              when (movementType) {
                MovementType.IN -> "Nama Pemasok / Ekspedisi"
                MovementType.OUT -> "Tujuan Pengiriman / Divisi Penerima"
                MovementType.ADJUST -> "Petugas Pemeriksa / Auditor"
              }
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_movement_party")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it.take(400) },
          label = { Text("Keterangan / Catatan Pemeriksaan") },
          maxLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_movement_notes")
        )

        if (errorText != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorText!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(onClick = onDismiss) {
            Text("Batal")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val target = selectedItem
              if (target == null) {
                errorText = "Silakan pilih barang terlebih dahulu."
                return@Button
              }
              val qty = quantityText.toLongOrNull()
              if (qty == null || (movementType != MovementType.ADJUST && qty <= 0L)) {
                errorText = "Masukkan jumlah barang yang valid."
                return@Button
              }
              if (movementType == MovementType.OUT && qty > target.quantity) {
                errorText = "Stok tidak mencukupi (tersedia ${target.quantity} ${target.unit})."
                return@Button
              }
              onConfirm(target, movementType, qty, referenceDoc, partyName, notes)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = accentColor
            ),
            modifier = Modifier.testTag("confirm_movement_button")
          ) {
            Text("Simpan Mutasi")
          }
        }
      }
    }
  }
}

@Composable
fun ConfirmDeleteDialog(
  item: InventoryItem,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Hapus Barang dari Gudang?") },
    text = {
      Text(
        "Apakah Anda yakin ingin menghapus '${item.name}' (${item.sku}) dari daftar inventaris gudang? Tindakan ini tidak dapat dibatalkan."
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_button")
      ) {
        Text("Hapus")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}
