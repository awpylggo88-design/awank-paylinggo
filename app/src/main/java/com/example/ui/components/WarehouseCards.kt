package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InventoryItem
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.ui.formatCompactRupiah
import com.example.ui.formatRupiah
import com.example.ui.formatTimestamp
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.InboundEmerald
import com.example.ui.theme.LowStockOrange
import com.example.ui.theme.OutboundBlue

@Composable
fun StatMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  Card(
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StockBadge(item: InventoryItem) {
  val (bgColor, textColor, statusText) = when {
    item.isOutOfStock -> Triple(
      CriticalCrimson.copy(alpha = 0.15f),
      CriticalCrimson,
      "STOK HABIS"
    )
    item.isLowStock -> Triple(
      LowStockOrange.copy(alpha = 0.16f),
      LowStockOrange,
      "MENIPIS (Min ${item.minStock})"
    )
    else -> Triple(
      InboundEmerald.copy(alpha = 0.14f),
      InboundEmerald,
      "AMAN"
    )
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(
      text = statusText,
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun InventoryItemCard(
  item: InventoryItem,
  onQuickIn: () -> Unit,
  onQuickOut: () -> Unit,
  onAdjustStock: () -> Unit,
  onEditItem: () -> Unit,
  onDeleteItem: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("inventory_card_${item.sku}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top Row: SKU badge + Category + Stock Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.sku,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.category,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
        StockBadge(item = item)
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Product Title & Current Stock Quantity
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Lokasi Rak",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = item.locationRack,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.supplierName.isNotBlank()) {
              Text(
                text = " • ${item.supplierName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${item.quantity} ${item.unit}",
            style = MaterialTheme.typography.headlineSmall,
            color = when {
              item.isOutOfStock -> CriticalCrimson
              item.isLowStock -> LowStockOrange
              else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${formatRupiah(item.unitPrice)} / ${item.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (item.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Catatan: ${item.notes}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Action Row: Total asset value + Quick Stock Buttons + Edit/Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Total Nilai Stok",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = formatCompactRupiah(item.totalValue),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilledTonalIconButton(
            onClick = onQuickIn,
            modifier = Modifier.testTag("btn_in_${item.sku}"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = InboundEmerald.copy(alpha = 0.14f),
              contentColor = InboundEmerald
            )
          ) {
            Icon(
              imageVector = Icons.Default.AddCircleOutline,
              contentDescription = "Barang Masuk"
            )
          }

          FilledTonalIconButton(
            onClick = onQuickOut,
            modifier = Modifier.testTag("btn_out_${item.sku}"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = OutboundBlue.copy(alpha = 0.14f),
              contentColor = OutboundBlue
            )
          ) {
            Icon(
              imageVector = Icons.Default.RemoveCircleOutline,
              contentDescription = "Barang Keluar"
            )
          }

          FilledTonalIconButton(
            onClick = onAdjustStock,
            modifier = Modifier.testTag("btn_opname_${item.sku}"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = LowStockOrange.copy(alpha = 0.15f),
              contentColor = LowStockOrange
            )
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Stok Opname"
            )
          }

          IconButton(
            onClick = onEditItem,
            modifier = Modifier.testTag("btn_edit_${item.sku}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Ubah Barang",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onDeleteItem,
            modifier = Modifier.testTag("btn_delete_${item.sku}")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Hapus Barang",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }
  }
}

@Composable
fun StockMovementRowCard(
  movement: StockMovement,
  modifier: Modifier = Modifier
) {
  val movType = movement.movementType
  val (accentColor, icon, signPrefix) = when (movType) {
    MovementType.IN -> Triple(InboundEmerald, Icons.Default.ArrowDownward, "+")
    MovementType.OUT -> Triple(OutboundBlue, Icons.Default.ArrowUpward, "-")
    MovementType.ADJUST -> Triple(LowStockOrange, Icons.Default.Tune, "±")
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = movType.label,
          tint = accentColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = movType.label,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Text(
            text = movement.referenceDoc,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${movement.itemName} (${movement.itemSku})",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold
        )
        if (movement.partyName.isNotBlank() || movement.notes.isNotBlank()) {
          val detailText = listOf(movement.partyName, movement.notes)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
          Text(
            text = detailText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = formatTimestamp(movement.createdAt),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "$signPrefix${movement.quantityChanged}",
          style = MaterialTheme.typography.titleLarge,
          color = accentColor,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${movement.previousStock} → ${movement.newStock}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
