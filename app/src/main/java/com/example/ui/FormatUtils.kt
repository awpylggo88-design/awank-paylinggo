package com.example.ui

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRupiah(amount: Long): String {
  val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
  format.maximumFractionDigits = 0
  return format.format(amount)
}

fun formatCompactRupiah(amount: Long): String {
  return when {
    amount >= 1_000_000_000L -> String.format(Locale("id", "ID"), "Rp %.1f M", amount / 1_000_000_000.0)
    amount >= 1_000_000L -> String.format(Locale("id", "ID"), "Rp %.1f Jt", amount / 1_000_000.0)
    amount >= 1_000L -> String.format(Locale("id", "ID"), "Rp %d Rb", amount / 1_000L)
    else -> formatRupiah(amount)
  }
}

fun formatTimestamp(timestamp: Timestamp?): String {
  val date: Date = timestamp?.toDate() ?: Date()
  val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
  return formatter.format(date)
}
