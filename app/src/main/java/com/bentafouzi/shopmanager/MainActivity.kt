package com.bentafouzi.shopmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.withTransaction
import com.bentafouzi.shopmanager.data.*
import com.bentafouzi.shopmanager.ui.theme.ShopTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = ShopDatabase.get(applicationContext)
        setContent {
            ShopTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ShopApp(database)
                }
            }
        }
    }
}

private enum class ShopTab(val title: String) {
    HOME("الرئيسية"), PRODUCTS("المنتجات"), SALES("المبيعات"), EXPENSES("المصاريف"), REPORTS("التقارير")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopApp(database: ShopDatabase) {
    val dao = database.dao()
    val products by dao.observeProducts().collectAsState(initial = emptyList())
    val sales by dao.observeSales().collectAsState(initial = emptyList())
    val expenses by dao.observeExpenses().collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf(ShopTab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المتجر") },
                actions = { Text("🛍️", modifier = Modifier.padding(horizontal = 16.dp)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                ShopTab.values().forEach { item ->
                    val icon = when (item) {
                        ShopTab.HOME -> Icons.Filled.Home
                        ShopTab.PRODUCTS -> Icons.Filled.Inventory2
                        ShopTab.SALES -> Icons.Filled.PointOfSale
                        ShopTab.EXPENSES -> Icons.Filled.Receipt
                        ShopTab.REPORTS -> Icons.Filled.BarChart
                    }
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(icon, contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            ShopTab.HOME -> DashboardScreen(products, sales, expenses, Modifier.padding(padding))
            ShopTab.PRODUCTS -> ProductsScreen(dao, products, snackbar, Modifier.padding(padding))
            ShopTab.SALES -> SalesScreen(database, products, sales, snackbar, Modifier.padding(padding))
            ShopTab.EXPENSES -> ExpensesScreen(dao, expenses, snackbar, Modifier.padding(padding))
            ShopTab.REPORTS -> ReportsScreen(sales, expenses, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DashboardScreen(products: List<Product>, sales: List<Sale>, expenses: List<Expense>, modifier: Modifier = Modifier) {
    val start = startOfToday()
    val todaySales = sales.filter { it.createdAt >= start }.sumOf { it.total }
    val todayExpenses = expenses.filter { it.createdAt >= start }.sumOf { it.amount }
    val lowStock = products.count { it.quantity <= 5 }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("ملخص اليوم", style = MaterialTheme.typography.headlineSmall) }
        item { SummaryCard("مبيعات اليوم", money(todaySales), Icons.Filled.TrendingUp) }
        item { SummaryCard("مصاريف اليوم", money(todayExpenses), Icons.Filled.Receipt) }
        item { SummaryCard("الربح التقريبي اليوم", money(todaySales - todayExpenses), Icons.Filled.AccountBalanceWallet) }
        item { SummaryCard("عدد المنتجات", products.size.toString(), Icons.Filled.Inventory2) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("تنبيه المخزون", style = MaterialTheme.typography.titleMedium)
                    Text(if (lowStock == 0) "المخزون جيد" else "يوجد $lowStock منتجات كميتها 5 أو أقل")
                }
            }
        }
        item { Text("آخر المبيعات", style = MaterialTheme.typography.titleLarge) }
        if (sales.isEmpty()) item { EmptyMessage("لا توجد مبيعات بعد") }
        items(sales.take(5), key = { "sale-${it.id}" }) { sale ->
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(sale.productName) },
                    supportingContent = { Text("${sale.quantity} × ${money(sale.total / sale.quantity)} · ${dateTime(sale.createdAt)}") },
                    trailingContent = { Text(money(sale.total), style = MaterialTheme.typography.titleSmall) }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ProductsScreen(dao: ShopDao, products: List<Product>, snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المنتجات (${products.size})", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { editing = null; showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = null); Text("إضافة") }
        }
        if (products.isEmpty()) EmptyMessage("أضف أول منتج إلى متجرك")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(products, key = { "product-${it.id}" }) { product ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = {
                            Column {
                                Text("${product.category} · المخزون: ${product.quantity}")
                                if (product.barcode.isNotBlank()) {
                                    Text(
                                        "الباركود: ${product.barcode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(product.price), style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = { editing = product; showDialog = true }) { Text("تعديل") }
                            }
                        }
                    )
                }
            }
        }
    }
    if (showDialog) {
        ProductDialog(initial = editing, onDismiss = { showDialog = false }) { name, category, price, quantity, barcode ->
            scope.launch {
                if (editing == null) dao.insertProduct(Product(name = name, category = category, price = price, quantity = quantity, barcode = barcode))
                else dao.updateProduct(editing!!.copy(name = name, category = category, price = price, quantity = quantity, barcode = barcode))
                showDialog = false
                snackbar.showSnackbar("تم حفظ المنتج")
            }
        }
    }
}

@Composable
private fun ProductDialog(initial: Product?, onDismiss: () -> Unit, onSave: (String, String, Double, Int, String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "عام") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString().orEmpty()) }
    var quantity by remember(initial) { mutableStateOf(initial?.quantity?.toString().orEmpty()) }
    var barcode by remember(initial) { mutableStateOf(initial?.barcode.orEmpty()) }
    var scanner by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0 && (quantity.toIntOrNull() ?: -1) >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة منتج" else "تعديل المنتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("الفئة") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("السعر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(quantity, { quantity = it }, label = { Text("الكمية في المخزون") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        barcode, { barcode = it },
                        label = { Text("الباركود (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(onClick = { scanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح الباركود")
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(name.trim(), category.trim().ifBlank { "عام" }, price.toDouble(), quantity.toInt(), barcode.trim()) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
    if (scanner) {
        BarcodeScannerDialog(
            onDismiss = { scanner = false },
            onCode = { code -> barcode = code; scanner = false }
        )
    }
}

@Composable
private fun SalesScreen(database: ShopDatabase, products: List<Product>, sales: List<Sale>, snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<Product?>(null) }
    var recherche by remember { mutableStateOf("") }
    var scanner by remember { mutableStateOf(false) }
    val filtres = if (recherche.isBlank()) products else products.filter {
        it.barcode.contains(recherche.trim()) || it.name.contains(recherche.trim(), ignoreCase = true)
    }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("بيع سريع", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                recherche, { recherche = it },
                label = { Text("ابحث بالباركود أو الاسم") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { scanner = true }) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح الباركود")
            }
        }
        if (products.isEmpty()) EmptyMessage("أضف منتجات قبل تسجيل المبيعات")
        else if (filtres.isEmpty()) EmptyMessage("لا يوجد منتج مطابق")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtres, key = { "sell-product-${it.id}" }) { product ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = { Text("المخزون: ${product.quantity}") },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(product.price))
                                Button(enabled = product.quantity > 0, onClick = { selected = product }) { Text("بيع") }
                            }
                        }
                    )
                }
            }
        }
        Text("سجل المبيعات", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        if (sales.isEmpty()) Text("لا توجد مبيعات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(sales.take(20), key = { "recent-sale-${it.id}" }) { sale ->
                ListItem(
                    headlineContent = { Text(sale.productName) },
                    supportingContent = { Text("الكمية: ${sale.quantity} · ${dateTime(sale.createdAt)}") },
                    trailingContent = { Text(money(sale.total)) }
                )
            }
        }
    }
    selected?.let { product ->
        QuantityDialog(product, onDismiss = { selected = null }) { quantity ->
            scope.launch {
                val result = database.withTransaction {
                    val changed = database.dao().decreaseStock(product.id, quantity)
                    if (changed > 0) database.dao().insertSale(
                        Sale(productId = product.id, productName = product.name, quantity = quantity, total = product.price * quantity)
                    )
                    changed
                }
                selected = null
                snackbar.showSnackbar(if (result > 0) "تم تسجيل البيع" else "الكمية غير متوفرة في المخزون")
            }
        }
    }
    if (scanner) {
        BarcodeScannerDialog(
            onDismiss = { scanner = false },
            onCode = { code ->
                scanner = false
                recherche = code
                val produit = products.firstOrNull { it.barcode == code }
                when {
                    produit == null -> scope.launch { snackbar.showSnackbar("لا يوجد منتج بهذا الباركود") }
                    produit.quantity <= 0 -> scope.launch { snackbar.showSnackbar("المخزون فارغ لهذا المنتج") }
                    else -> selected = produit
                }
            }
        )
    }
}

@Composable
private fun QuantityDialog(product: Product, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var quantity by remember(product.id) { mutableStateOf("1") }
    val amount = quantity.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل بيع: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("السعر: ${money(product.price)} · المتوفر: ${product.quantity}")
                OutlinedTextField(quantity, { quantity = it }, label = { Text("الكمية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                if (amount != null && amount > 0) Text("الإجمالي: ${money(product.price * amount)}")
            }
        },
        confirmButton = { TextButton(enabled = amount != null && amount in 1..product.quantity, onClick = { onConfirm(amount!!) }) { Text("تأكيد البيع") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun BarcodeScannerDialog(onDismiss: () -> Unit, onCode: (String) -> Unit) {
    val contexte = LocalContext.current
    var accordee by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var erreur by remember { mutableStateOf<String?>(null) }
    val demandeur = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee = it }
    LaunchedEffect(Unit) { if (!accordee) demandeur.launch(Manifest.permission.CAMERA) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("مسح الباركود", style = MaterialTheme.typography.titleLarge)
                if (accordee) {
                    Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp))) {
                        CameraPreview(onCode = onCode, onError = { erreur = it })
                    }
                    Text(
                        erreur ?: "وجّه الكاميرا نحو الباركود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("يجب السماح باستخدام الكاميرا لمسح الباركود.")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!accordee) {
                        TextButton(onClick = { demandeur.launch(Manifest.permission.CAMERA) }) { Text("السماح") }
                    }
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onCode: (String) -> Unit, onError: (String) -> Unit) {
    val contexte = LocalContext.current
    val proprietaire = LocalLifecycleOwner.current
    val analyseur = remember { BarcodeAnalyser(onCode) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val fournisseur = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            fournisseur.value?.unbindAll()
            analyseur.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vue = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val futur = ProcessCameraProvider.getInstance(ctx)
            futur.addListener({
                try {
                    val provider = futur.get()
                    fournisseur.value = provider
                    val apercu = Preview.Builder().build()
                        .also { it.setSurfaceProvider(vue.surfaceProvider) }
                    val analyse = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyseur) }
                    provider.unbindAll()
                    provider.bindToLifecycle(proprietaire, CameraSelector.DEFAULT_BACK_CAMERA, apercu, analyse)
                } catch (t: Throwable) {
                    onError("الكاميرا غير متاحة على هذا الجهاز")
                }
            }, ContextCompat.getMainExecutor(ctx))
            vue
        }
    )
}

private class BarcodeAnalyser(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val lecteur = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )
    private val dejaLu = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val entree = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        lecteur.process(entree)
            .addOnSuccessListener { codes ->
                val valeur = codes.mapNotNull { it.rawValue }.firstOrNull { it.isNotBlank() }
                if (valeur != null && dejaLu.compareAndSet(false, true)) onCode(valeur)
            }
            .addOnCompleteListener { image.close() }
    }

    fun close() {
        lecteur.close()
    }
}

@Composable
private fun ExpensesScreen(dao: ShopDao, expenses: List<Expense>, snackbar: SnackbarHostState, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المصاريف", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showDialog = true }) { Icon(Icons.Filled.Add, contentDescription = null); Text("إضافة") }
        }
        if (expenses.isEmpty()) EmptyMessage("لا توجد مصاريف مسجلة")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses, key = { "expense-${it.id}" }) { expense ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(expense.title) },
                        supportingContent = { Text(dateTime(expense.createdAt)) },
                        trailingContent = { Text(money(expense.amount), style = MaterialTheme.typography.titleSmall) }
                    )
                }
            }
        }
    }
    if (showDialog) ExpenseDialog(
        onDismiss = { showDialog = false },
        onSave = { title, amount ->
            scope.launch {
                dao.insertExpense(Expense(title = title, amount = amount))
                showDialog = false
                snackbar.showSnackbar("تم حفظ المصروف")
            }
        }
    )
}

@Composable
private fun ExpenseDialog(onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val valid = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مصروف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("وصف المصروف") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(title.trim(), amount.toDouble()) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ReportsScreen(sales: List<Sale>, expenses: List<Expense>, modifier: Modifier = Modifier) {
    val totalSales = sales.sumOf { it.total }
    val totalExpenses = expenses.sumOf { it.amount }
    val today = startOfToday()
    val daySales = sales.filter { it.createdAt >= today }.sumOf { it.total }
    val dayExpenses = expenses.filter { it.createdAt >= today }.sumOf { it.amount }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("التقارير", style = MaterialTheme.typography.headlineSmall) }
        item { SummaryCard("إجمالي المبيعات", money(totalSales), Icons.Filled.TrendingUp) }
        item { SummaryCard("إجمالي المصاريف", money(totalExpenses), Icons.Filled.Receipt) }
        item { SummaryCard("صافي الربح التقريبي", money(totalSales - totalExpenses), Icons.Filled.AccountBalanceWallet) }
        item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
        item { Text("ملخص اليوم", style = MaterialTheme.typography.titleLarge) }
        item { ReportLine("المبيعات", money(daySales)) }
        item { ReportLine("المصاريف", money(dayExpenses)) }
        item { ReportLine("الصافي", money(daySales - dayExpenses)) }
        item { Text("عدد عمليات البيع: ${sales.size}") }
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun money(value: Double): String =
    NumberFormat.getNumberInstance(Locale("ar", "MA")).apply { maximumFractionDigits = 2 }.format(value) + " د.م."

private fun dateTime(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar", "MA")).format(Date(timestamp))

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
