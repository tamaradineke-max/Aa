package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.VintedStudioViewModel
import com.example.ui.screens.ReferencesScreen
import com.example.ui.screens.ShootDetailScreen
import com.example.ui.screens.ShootsHistoryScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.VintedStudioTheme
import kotlinx.coroutines.launch

const val ROUTE_STUDIO = "studio"
const val ROUTE_SHOOTS = "shoots"
const val ROUTE_REFERENCES = "references"
const val ROUTE_SHOOT_DETAIL = "shoot_detail/{shootId}"

private data class StudioDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topDestinations = listOf(
    StudioDestination(
        route = ROUTE_STUDIO,
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        testTag = "nav_studio"
    ),
    StudioDestination(
        route = ROUTE_SHOOTS,
        labelRes = R.string.nav_shoots,
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections,
        testTag = "nav_shoots"
    ),
    StudioDestination(
        route = ROUTE_REFERENCES,
        labelRes = R.string.nav_references,
        selectedIcon = Icons.Filled.Face,
        unselectedIcon = Icons.Outlined.Face,
        testTag = "nav_references"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            VintedStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VintedStudioApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VintedStudioApp() {
    val viewModel: VintedStudioViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetailRoute = currentDestination?.route?.startsWith("shoot_detail") == true

    val activeState by viewModel.activeState.collectAsStateWithLifecycle()
    val savedShoots by viewModel.savedShoots.collectAsStateWithLifecycle()
    val customModelRefPath by viewModel.customModelRefPath.collectAsStateWithLifecycle()
    val customBedroomRefPath by viewModel.customBedroomRefPath.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    val handleSaveToGallery: (String, String) -> Unit = { filePath, displayName ->
        coroutineScope.launch {
            val saved = viewModel.mediaStorageHelper.saveImageFileToGallery(filePath, displayName)
            showMessage(
                if (saved) "Saved photo to Pictures/VintedStudio"
                else "Could not save photo to gallery"
            )
        }
    }

    val handleShareImage: (String, String) -> Unit = { filePath, title ->
        val shareIntent = viewModel.mediaStorageHelper.buildShareImageIntent(filePath, title)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, title))
        } else {
            showMessage("Image file unavailable for sharing")
        }
    }

    val handleCopyText: (String, String) -> Unit = { label, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        showMessage("$label gekopieerd naar klembord")
    }

    val handleShareAdText: (String) -> Unit = { text ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Deel Vinted advertentie"))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isDetailRoute) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && !isDetailRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isDetailRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            activeState = activeState,
                            customModelRefPath = customModelRefPath,
                            customBedroomRefPath = customBedroomRefPath,
                            onAddGarmentUris = viewModel::addGarmentUris,
                            onRemoveGarmentUri = viewModel::removeGarmentUri,
                            onUpdateNotes = { brand, size, cond, room ->
                                viewModel.updateUserNotes(brand, size, cond, room)
                            },
                            onStartShoot = viewModel::startFullStudioShoot,
                            onStartAdOnly = viewModel::startAdOnlyShoot,
                            onResetStudio = viewModel::resetActiveStudio,
                            onRegeneratePose = viewModel::regenerateSinglePose,
                            onRegenerateAd = viewModel::regenerateDutchAd,
                            onOpenReferencesTab = {
                                navController.navigate(ROUTE_REFERENCES) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSaveToGallery = handleSaveToGallery,
                            onShareImage = handleShareImage,
                            onCopyText = handleCopyText,
                            onShareAdText = handleShareAdText
                        )
                    }

                    composable(ROUTE_SHOOTS) {
                        ShootsHistoryScreen(
                            shoots = savedShoots,
                            onOpenShoot = { shootId ->
                                navController.navigate("shoot_detail/$shootId")
                            },
                            onDeleteShoot = { shootId ->
                                viewModel.deleteShoot(shootId)
                                showMessage("Shoot removed")
                            }
                        )
                    }

                    composable(ROUTE_REFERENCES) {
                        ReferencesScreen(
                            customModelRefPath = customModelRefPath,
                            customBedroomRefPath = customBedroomRefPath,
                            onChangeModelRef = { uri ->
                                viewModel.updateCustomModelReference(uri)
                                showMessage("Updated Model Identity Reference")
                            },
                            onResetModelRef = {
                                viewModel.resetModelReferenceToDefault()
                                showMessage("Restored default brunette model reference")
                            },
                            onChangeBedroomRef = { uri ->
                                viewModel.updateCustomBedroomReference(uri)
                                showMessage("Updated Bedroom Setting Reference")
                            },
                            onResetBedroomRef = {
                                viewModel.resetBedroomReferenceToDefault()
                                showMessage("Restored default Dutch bedroom reference")
                            }
                        )
                    }

                    composable(
                        route = ROUTE_SHOOT_DETAIL,
                        arguments = listOf(navArgument("shootId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val shootId = backStackEntry.arguments?.getString("shootId").orEmpty()
                        val shoot by viewModel.observeShootById(shootId).collectAsStateWithLifecycle()
                        ShootDetailScreen(
                            shoot = shoot,
                            onBack = { navController.popBackStack() },
                            onSaveToGallery = handleSaveToGallery,
                            onShareImage = handleShareImage,
                            onCopyText = handleCopyText,
                            onShareAdText = handleShareAdText
                        )
                    }
                }
            }
        }
    }
}
