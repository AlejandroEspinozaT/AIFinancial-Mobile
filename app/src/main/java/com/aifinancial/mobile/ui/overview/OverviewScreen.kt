package com.aifinancial.mobile.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinancial.mobile.data.local.CachedBudgetEntity
import com.aifinancial.mobile.data.local.CachedTransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = hiltViewModel()) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetsLastUpdated by viewModel.budgetsLastUpdated.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val transactionsLastUpdated by viewModel.transactionsLastUpdated.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            SectionHeader(title = "Budgets", lastUpdated = budgetsLastUpdated)
        }
        if (budgets.isEmpty()) {
            item { EmptyCacheMessage(hasEverSynced = budgetsLastUpdated != null) }
        } else {
            items(budgets, key = { it.id }) { budget ->
                BudgetRow(budget)
                HorizontalDivider()
            }
        }

        item {
            Spacer(modifier = Modifier.padding(top = 16.dp))
            SectionHeader(title = "Recent transactions", lastUpdated = transactionsLastUpdated)
        }
        if (transactions.isEmpty()) {
            item { EmptyCacheMessage(hasEverSynced = transactionsLastUpdated != null) }
        } else {
            items(transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, lastUpdated: Instant?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            // Stage 3's "staleness is visible, not hidden": this label is
            // never skipped, even when there's data to show — a cached list
            // is never presented as indistinguishable from live data.
            text = lastUpdatedLabel(lastUpdated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyCacheMessage(hasEverSynced: Boolean) {
    Text(
        text = if (hasEverSynced) {
            "Nothing here yet."
        } else {
            "Not loaded yet — connect once to load this."
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun BudgetRow(budget: CachedBudgetEntity) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(budget.category, style = MaterialTheme.typography.bodyMedium)
        Text(
            "%.2f / %.2f (%.0f%%)".format(budget.amountSpent, budget.monthlyLimit, budget.percentUtilized),
            style = MaterialTheme.typography.bodySmall,
            color = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionRow(transaction: CachedTransactionEntity) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${transaction.date} · ${transaction.category} · ${transaction.amount} ${transaction.currency}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun lastUpdatedLabel(lastUpdated: Instant?): String {
    if (lastUpdated == null) return "Last updated: never"
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    return "Last updated: ${formatter.format(lastUpdated)}"
}
