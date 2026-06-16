package com.example.pantrypal.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pantrypal.core.designsystem.PantryBottomBar
import com.example.pantrypal.core.designsystem.PantryBottomBarItem
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.feature.addfood.AddFoodEffect
import com.example.pantrypal.feature.addfood.AddFoodViewModel
import com.example.pantrypal.feature.addfood.ManualAddEvent
import com.example.pantrypal.feature.addfood.ManualAddScreen
import com.example.pantrypal.feature.addfood.ScanBarcodeScreen
import com.example.pantrypal.feature.addfood.ScanEvent
import com.example.pantrypal.feature.home.HomeEffect
import com.example.pantrypal.feature.home.HomeScreen
import com.example.pantrypal.feature.home.HomeViewModel
import com.example.pantrypal.feature.pantry.FoodDetailEffect
import com.example.pantrypal.feature.pantry.FoodDetailScreen
import com.example.pantrypal.feature.pantry.FoodDetailViewModel
import com.example.pantrypal.feature.pantry.FoodLinksScreen
import com.example.pantrypal.feature.pantry.PantryEffect
import com.example.pantrypal.feature.pantry.PantryScreen
import com.example.pantrypal.feature.pantry.PantryViewModel
import com.example.pantrypal.feature.profile.ProfileScreen
import com.example.pantrypal.feature.profile.ProfileViewModel
import com.example.pantrypal.feature.recipes.RecipeDetailEffect
import com.example.pantrypal.feature.recipes.RecipeDetailScreen
import com.example.pantrypal.feature.recipes.RecipeDetailViewModel
import com.example.pantrypal.feature.recipes.RecipesEffect
import com.example.pantrypal.feature.recipes.RecipesScreen
import com.example.pantrypal.feature.recipes.RecipesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryPalRoot() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showAddChoice by rememberSaveable { mutableStateOf(false) }
    var addOriginRoute by rememberSaveable { mutableStateOf(AppRoute.Home.route) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!currentRoute.isAddFlowRoute()) {
                PantryBottomBar(
                    items = bottomBarItems,
                    currentRoute = currentRoute.toSelectedTabRoute(),
                    onItemClick = { route -> navController.navigateMainTab(route) },
                    onFabClick = {
                        addOriginRoute = currentRoute.toSelectedTabRoute() ?: AppRoute.Home.route
                        showAddChoice = true
                    }
                )
            }
        },
        containerColor = PantryColors.Background
    ) { innerPadding ->
        PantryNavHost(
            navController = navController,
            snackbarHostState = snackbarHostState,
            addOriginRoute = addOriginRoute,
            showSnackbar = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
            openAddChoice = {
                addOriginRoute = currentRoute.toSelectedTabRoute() ?: AppRoute.Home.route
                showAddChoice = true
            },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showAddChoice) {
        ModalBottomSheet(onDismissRequest = { showAddChoice = false }, containerColor = PantryColors.Card) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(PantrySpacing.md)
            ) {
                Text("Aggiungi alimento", style = PantryTypography.headlineMedium)
                Text("Scegli come iniziare il flow di inserimento.", color = PantryColors.Muted)
                Button(
                    onClick = {
                        showAddChoice = false
                        navController.navigate(AppRoute.Scan.route)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700)
                ) {
                    Text("Scansiona barcode")
                }
                OutlinedButton(
                    onClick = {
                        showAddChoice = false
                        navController.navigate(AppRoute.ManualAdd.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inserisci manualmente")
                }
            }
        }
    }
}

@Composable
private fun PantryNavHost(
    navController: androidx.navigation.NavHostController,
    snackbarHostState: SnackbarHostState,
    addOriginRoute: String,
    showSnackbar: (String) -> Unit,
    openAddChoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier
    ) {
        composable(AppRoute.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        HomeEffect.OpenAddChoiceSheet -> openAddChoice()
                        is HomeEffect.NavigateToPantry -> navController.navigateMainTab(AppRoute.Pantry.route)
                        is HomeEffect.NavigateToRecipeDetail -> navController.navigate(AppRoute.RecipeDetail.create(effect.recipeId))
                    }
                }
            }
            HomeScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(AppRoute.Pantry.route) {
            val viewModel: PantryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        PantryEffect.OpenAddChoiceSheet -> openAddChoice()
                        is PantryEffect.NavigateToFoodDetail -> navController.navigate(AppRoute.FoodDetail.create(effect.categoryId))
                        is PantryEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
            PantryScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(AppRoute.Recipes.route) {
            val viewModel: RecipesViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is RecipesEffect.NavigateToRecipeDetail -> navController.navigate(AppRoute.RecipeDetail.create(effect.externalId))
                    }
                }
            }
            RecipesScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(AppRoute.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(AppRoute.Scan.route) {
            val viewModel: AddFoodViewModel = hiltViewModel()
            val state by viewModel.scanState.collectAsStateWithLifecycle()
            BackHandler { navController.popBackStack(addOriginRoute, false) }
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    handleAddFoodEffect(effect, navController, addOriginRoute, showSnackbar)
                }
            }
            ScanBarcodeScreen(state = state, onEvent = viewModel::onScanEvent)
        }

        composable(AppRoute.ManualAdd.route) {
            val viewModel: AddFoodViewModel = hiltViewModel()
            val state by viewModel.manualState.collectAsStateWithLifecycle()
            BackHandler { navController.popBackStack(addOriginRoute, false) }
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    handleAddFoodEffect(effect, navController, addOriginRoute, showSnackbar)
                }
            }
            ManualAddScreen(state = state, onEvent = viewModel::onManualEvent)
        }

        composable(
            route = AppRoute.FoodDetail.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) {
            FoodDetailRoute(
                navController = navController,
                snackbarHostState = snackbarHostState,
                showLinks = false
            )
        }

        composable(
            route = AppRoute.FoodLinks.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) {
            FoodDetailRoute(
                navController = navController,
                snackbarHostState = snackbarHostState,
                showLinks = true
            )
        }

        composable(
            route = AppRoute.RecipeDetail.route,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) {
            val viewModel: RecipeDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        RecipeDetailEffect.NavigateBack -> navController.popBackStack()
                        is RecipeDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
            RecipeDetailScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}

@Composable
private fun FoodDetailRoute(
    navController: androidx.navigation.NavHostController,
    snackbarHostState: SnackbarHostState,
    showLinks: Boolean
) {
    val viewModel: FoodDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FoodDetailEffect.NavigateBack -> navController.popBackStack()
                FoodDetailEffect.NavigateToLinks -> navController.navigate(AppRoute.FoodLinks.create(state.categoryId))
                is FoodDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    if (showLinks) {
        FoodLinksScreen(state = state, onEvent = viewModel::onEvent)
    } else {
        FoodDetailScreen(state = state, onEvent = viewModel::onEvent)
    }
}

private fun handleAddFoodEffect(
    effect: AddFoodEffect,
    navController: androidx.navigation.NavHostController,
    addOriginRoute: String,
    showSnackbar: (String) -> Unit
) {
    when (effect) {
        AddFoodEffect.FinishAddFlow -> navController.popBackStack(addOriginRoute, false)
        AddFoodEffect.NavigateToManualAdd -> navController.navigate(AppRoute.ManualAdd.route) {
            popUpTo(AppRoute.Scan.route) { inclusive = true }
        }
        is AddFoodEffect.ShowSnackbar -> showSnackbar(effect.message)
    }
}

private fun androidx.navigation.NavHostController.navigateMainTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

private fun String?.toSelectedTabRoute(): String? =
    when {
        this == null -> null
        this == AppRoute.Home.route -> AppRoute.Home.route
        this == AppRoute.Pantry.route || this.startsWith("food/") || this.startsWith("food/{") -> AppRoute.Pantry.route
        this == AppRoute.Recipes.route || this.startsWith("recipe/") || this.startsWith("recipe/{") -> AppRoute.Recipes.route
        this == AppRoute.Profile.route -> AppRoute.Profile.route
        else -> this
    }

private val bottomBarItems = listOf(
    PantryBottomBarItem(AppRoute.Home.route, "Home", Icons.Default.Home),
    PantryBottomBarItem(AppRoute.Pantry.route, "Dispensa", Icons.Default.Inventory2),
    PantryBottomBarItem(AppRoute.Recipes.route, "Ricette", Icons.Default.Restaurant),
    PantryBottomBarItem(AppRoute.Profile.route, "Profilo", Icons.Default.Person)
)
