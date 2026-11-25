package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.VehicleRecord
import com.example.ui.GatePassViewModel
import com.example.ui.RegistrationPrefill
import com.example.ui.StatusFilter
import com.example.ui.screens.CsvGuideDialog
import com.example.ui.screens.CsvOutcomeDialog
import com.example.ui.screens.CsvPreviewImportDialog
import com.example.ui.screens.RegisterFormScreen
import com.example.ui.screens.RegistryScreen
import com.example.ui.screens.VehicleDetailDialog
import com.example.ui.screens.VerifyScreen
import com.example.ui.theme.GatePassTheme
import com.example.util.DateUtils

const val ROUTE_VERIFY = "verify"
const val ROUTE_REGISTRY = "registry"
const val ROUTE_REGISTER = "register"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      GatePassTheme {
        GatePassApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatePassApp(
  viewModel: GatePassViewModel = viewModel(),
) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_VERIFY

  val stats by viewModel.registryStats.collectAsStateWithLifecycle()
  val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
  val filterState by viewModel.filterState.collectAsStateWithLifecycle()
  val verificationResult by viewModel.verificationResult.collectAsStateWithLifecycle()
  val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
  val pendingCsvParse by viewModel.pendingCsvParse.collectAsStateWithLifecycle()
  val lastImportOutcome by viewModel.lastImportOutcome.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  var showCsvGuideDialog by remember { mutableStateOf(false) }
  var selectedDetailRecord by remember { mutableStateOf<VehicleRecord?>(null) }
  var formPrefill by remember { mutableStateOf(RegistrationPrefill()) }

  val csvOpenLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.parseCsvFromUri(context.contentResolver, uri)
      }
    }

  val csvTemplateLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
        viewModel.writeSampleCsvTemplate(context.contentResolver, uri)
      }
    }

  LaunchedEffect(snackbarMessage) {
    val msg = snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  val openNewRegistration: (RegistrationPrefill) -> Unit = { prefill ->
    formPrefill = prefill
    navController.navigate(ROUTE_REGISTER)
  }

  val openEditRegistration: (VehicleRecord) -> Unit = { record ->
    formPrefill =
      RegistrationPrefill(
        existingRecordId = record.id,
        driverName = record.driverName,
        licensePlate = record.licensePlate,
        driverLicenseNumber = record.driverLicenseNumber,
        phoneNumber = record.phoneNumber,
        stationLocation = record.stationLocation,
        vehicleType = record.vehicleType,
        registrationDateIso = record.registrationDateIso,
        expirationDateIso = record.expirationDateIso,
        internalRegCode = record.internalRegCode,
      )
    navController.navigate(ROUTE_REGISTER)
  }

  if (showCsvGuideDialog) {
    CsvGuideDialog(
      onPickCsvClick = {
        showCsvGuideDialog = false
        csvOpenLauncher.launch(
          arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "*/*")
        )
      },
      onSaveTemplateClick = {
        showCsvGuideDialog = false
        csvTemplateLauncher.launch("driver_registration_template.csv")
      },
      onDismiss = { showCsvGuideDialog = false },
    )
  }

  pendingCsvParse?.let { parsed ->
    CsvPreviewImportDialog(
      parseResult = parsed,
      onConfirmImport = { viewModel.confirmCsvImport() },
      onDismiss = { viewModel.dismissCsvDialogs() },
    )
  }

  lastImportOutcome?.let { outcome ->
    CsvOutcomeDialog(
      outcome = outcome,
      onDismiss = { viewModel.dismissCsvDialogs() },
    )
  }

  selectedDetailRecord?.let { detail ->
    VehicleDetailDialog(
      record = detail,
      onDismiss = { selectedDetailRecord = null },
      onRenew = { record ->
        viewModel.renewRecordOneYear(record)
        selectedDetailRecord = null
      },
      onEdit = { record ->
        selectedDetailRecord = null
        openEditRegistration(record)
      },
      onDelete = { record ->
        viewModel.deleteRecord(record)
        selectedDetailRecord = null
      },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 700.dp
    val showChrome = currentRoute != ROUTE_REGISTER

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavRail && showChrome) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Spacer(modifier = Modifier.size(16.dp))
          NavigationRailItem(
            selected = currentRoute == ROUTE_VERIFY,
            onClick = {
              navController.navigate(ROUTE_VERIFY) {
                popUpTo(ROUTE_VERIFY) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = {
              Icon(
                imageVector =
                  if (currentRoute == ROUTE_VERIFY) Icons.Filled.QrCodeScanner
                  else Icons.Outlined.QrCodeScanner,
                contentDescription = "Verify",
              )
            },
            label = { Text("Verify") },
            modifier = Modifier.testTag("nav_verify"),
          )
          NavigationRailItem(
            selected = currentRoute == ROUTE_REGISTRY,
            onClick = {
              navController.navigate(ROUTE_REGISTRY) {
                popUpTo(ROUTE_VERIFY) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = {
              Icon(
                imageVector =
                  if (currentRoute == ROUTE_REGISTRY) Icons.Filled.Badge
                  else Icons.Outlined.Badge,
                contentDescription = "Registry",
              )
            },
            label = { Text("Registry") },
            modifier = Modifier.testTag("nav_registry"),
          )
        }
      }

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          if (showChrome) {
            CenterAlignedTopAppBar(
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                }
              },
              actions = {
                if (currentRoute == ROUTE_VERIFY) {
                  IconButton(
                    onClick = {
                      val today = DateUtils.todayIso()
                      openNewRegistration(
                        RegistrationPrefill(
                          registrationDateIso = today,
                          expirationDateIso = DateUtils.addOneYearIso(today),
                          internalRegCode = DateUtils.generateInternalRegCode("UMAT", today),
                        )
                      )
                    },
                    modifier = Modifier.testTag("topbar_register_driver"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.PersonAdd,
                      contentDescription = "Register driver",
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.padding(end = 12.dp),
                ) {
                  Text(
                    text = "OFFLINE DB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              },
            )
          }
        },
        bottomBar = {
          if (!useNavRail && showChrome) {
            NavigationBar {
              NavigationBarItem(
                selected = currentRoute == ROUTE_VERIFY,
                onClick = {
                  navController.navigate(ROUTE_VERIFY) {
                    popUpTo(ROUTE_VERIFY) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector =
                      if (currentRoute == ROUTE_VERIFY) Icons.Filled.QrCodeScanner
                      else Icons.Outlined.QrCodeScanner,
                    contentDescription = "Verify",
                  )
                },
                label = { Text("Verify") },
                modifier = Modifier.testTag("nav_verify"),
              )
              NavigationBarItem(
                selected = currentRoute == ROUTE_REGISTRY,
                onClick = {
                  navController.navigate(ROUTE_REGISTRY) {
                    popUpTo(ROUTE_VERIFY) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector =
                      if (currentRoute == ROUTE_REGISTRY) Icons.Filled.Badge
                      else Icons.Outlined.Badge,
                    contentDescription = "Registry",
                  )
                },
                label = { Text("Registry (${stats.total})") },
                modifier = Modifier.testTag("nav_registry"),
              )
            }
          }
        },
        floatingActionButton = {
          if (currentRoute == ROUTE_REGISTRY) {
            ExtendedFloatingActionButton(
              onClick = {
                val today = DateUtils.todayIso()
                openNewRegistration(
                  RegistrationPrefill(
                    registrationDateIso = today,
                    expirationDateIso = DateUtils.addOneYearIso(today),
                    internalRegCode = DateUtils.generateInternalRegCode("UMAT", today),
                  )
                )
              },
              icon = { Icon(Icons.Filled.Add, contentDescription = null) },
              text = { Text("Register") },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_register_driver"),
            )
          }
        },
        modifier = Modifier.weight(1f),
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = ROUTE_VERIFY,
          modifier = Modifier.fillMaxSize(),
        ) {
          composable(ROUTE_VERIFY) {
            VerifyScreen(
              stats = stats,
              verificationResult = verificationResult,
              recentScans = recentScans,
              onVerifyQuery = { raw, fromCam ->
                viewModel.verifyScannedCode(raw, fromCam)
              },
              onClearResult = { viewModel.clearVerificationResult() },
              onPhotoError = { err -> viewModel.showMessage(err) },
              onRenewRecord = { record -> viewModel.renewRecordOneYear(record) },
              onViewPass = { record -> selectedDetailRecord = record },
              onEditRecord = { record -> openEditRegistration(record) },
              onRegisterPrefilled = { prefill -> openNewRegistration(prefill) },
              onFilterExpiredClick = {
                viewModel.updateStatusFilter(StatusFilter.EXPIRED)
                navController.navigate(ROUTE_REGISTRY) {
                  popUpTo(ROUTE_VERIFY) { saveState = true }
                  launchSingleTop = true
                }
              },
              contentPadding = innerPadding,
            )
          }

          composable(ROUTE_REGISTRY) {
            RegistryScreen(
              records = filteredRecords,
              stats = stats,
              filterState = filterState,
              onSearchQueryChange = { viewModel.updateSearchQuery(it) },
              onSearchFieldChange = { viewModel.updateSearchField(it) },
              onStatusFilterChange = { viewModel.updateStatusFilter(it) },
              onExpirationDateChange = { viewModel.updateExpirationDateFilter(it) },
              onDateFilterModeChange = { viewModel.updateDateFilterMode(it) },
              onSortOrderChange = { viewModel.updateSortOrder(it) },
              onClearFilters = { viewModel.clearAllFilters() },
              onRecordClick = { record -> selectedDetailRecord = record },
              onAddDriverClick = {
                val today = DateUtils.todayIso()
                openNewRegistration(
                  RegistrationPrefill(
                    registrationDateIso = today,
                    expirationDateIso = DateUtils.addOneYearIso(today),
                    internalRegCode = DateUtils.generateInternalRegCode("UMAT", today),
                  )
                )
              },
              onImportCsvClick = { showCsvGuideDialog = true },
              onExportTemplateClick = {
                csvTemplateLauncher.launch("driver_registration_template.csv")
              },
              contentPadding = innerPadding,
            )
          }

          composable(ROUTE_REGISTER) {
            RegisterFormScreen(
              prefill = formPrefill,
              onNavigateBack = { navController.popBackStack() },
              onSave = {
                existingId,
                driverName,
                licensePlate,
                regDateIso,
                expDateIso,
                driverLicenseNumber,
                phoneNumber,
                stationLocation,
                internalRegCode,
                vehicleType,
                ->
                viewModel.saveDriverRecord(
                  existingId = existingId,
                  driverName = driverName,
                  licensePlate = licensePlate,
                  registrationDateIso = regDateIso,
                  expirationDateIso = expDateIso,
                  driverLicenseNumber = driverLicenseNumber,
                  phoneNumber = phoneNumber,
                  stationLocation = stationLocation,
                  internalRegCode = internalRegCode,
                  vehicleType = vehicleType,
                ) {
                  navController.popBackStack()
                }
              },
            )
          }
        }
      }
    }
  }
}
