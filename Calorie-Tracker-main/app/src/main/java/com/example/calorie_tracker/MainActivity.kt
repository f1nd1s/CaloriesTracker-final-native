package com.example.calorie_tracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.withContext

var currentlySelectedMeal: MealTypes? = null
val server = Server()
val currentDateAndWieght: dateAndWieght? = null
var currentColorTheme: ColorTheme = ColorTheme.DARK // Defaults to dark for battery saving
// local variable of meal Info so that it is cached locally
val mealInfoList = MutableList(5) { TotalMealInfo("0", "0", "0", "0") }

// local versions of total meal information for caching AddFood UI
lateinit var breakfastInformation: AddFoodPage.MealInformation
lateinit var lunchInformation: AddFoodPage.MealInformation
lateinit var dinnerInformation: AddFoodPage.MealInformation
lateinit var desertInformation: AddFoodPage.MealInformation
lateinit var snacksInformation: AddFoodPage.MealInformation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Main(this)
        }
    }
}

@Composable
fun Main(context: Context) {
    val dbman = DatabaseManager(context)
    val navController = rememberNavController()
    // Pass this into each UI so it can be accessed once in each page and correctly keep theme
    val myColorThemeViewModel: ColorThemeViewModel = viewModel()
    val databasedColorTheme = dbman.checkIfExistingColorTheme()

    // if color them previously selected, then load desired theme on startup
    if (databasedColorTheme != -1) {
        val selectedTheme = ColorTheme.entries.getOrNull(databasedColorTheme)
            ?: ColorTheme.DARK // elvis operator to default to DARK if nothing exists in table
        myColorThemeViewModel.currentColorTheme =
            selectedTheme // cached locally outside database
    }

    // caches all the information from the database for power saving and locality
    breakfastInformation = GetCache(dbman, "Breakfast")
    lunchInformation = GetCache(dbman, "Lunch")
    dinnerInformation = GetCache(dbman, "Dinner")
    desertInformation = GetCache(dbman, "Desert")
    snacksInformation = GetCache(dbman, "Snacks")

    // The navHost which controls the different screens that actually shown to the end user
    NavHost(navController, startDestination = Screens.MAINSCREEN.name) {
        composable(Screens.MAINSCREEN.name) {
            MainScreenUI(navController, myColorThemeViewModel, dbman)
        }

        composable(Screens.FOODPAGE.name) {
            val foodPage = AddFoodPage()
            foodPage.FoodPageUI(navController, correctFoodCapitalization(currentlySelectedMeal.toString()), myColorThemeViewModel, dbman)
        }

        composable(Screens.GRAPHPAGE.name) {
            val graphPage = viewGraphPage()
            graphPage.ComplexGraphController(navController, myColorThemeViewModel, dbman)
        }

        composable(Screens.PROFILEPAGE.name) {
            val ProfilePage = ProfilePage()
            ProfilePage.ProfilePageUI(navController, myColorThemeViewModel, server, dbman)
        }
    }
}

// The main screen UI composable which holds the summary information, different rows for meals,
// navigation to other screens, and color schemes
@Composable
fun MainScreenUI(navController: NavHostController, myColorThemeViewModel: ColorThemeViewModel, dbman: DatabaseManager) {

    checkPreviousDates(dbman)  // log current time against previous database times to see if its a new day or not
    var currentColorTheme = myColorThemeViewModel.currentColorTheme
    packMealInfoList(dbman)
    val recommendedCalories = dbman.getRecommendedCalories()

    Column (
        modifier = Modifier.fillMaxSize().background(currentColorTheme.backgroundColor)
    ) {
        // row for top buttons and the the current streak
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp)
        ) { // row for streak a and account button
            Text("CalorieWatchers",
                fontSize = 15.sp,
                color = currentColorTheme.textColor,
                modifier = Modifier.align(Alignment.CenterVertically).weight(1f)
            )
            // call function that controls dropdown menu in top right of app
            myNavhostDropDownMenu(navController, myColorThemeViewModel)
        }

        // row housing box that will be used to show calories, fat, protein, and carbs remaining
        Row(modifier = Modifier.fillMaxWidth().weight(8f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) { // words before Summary Box
                // Summary text for user understanding
                Row {
                    Text("Summary: ",
                        fontSize = 30.sp,
                        color = currentColorTheme.textColor,
                        modifier = Modifier.align(Alignment.CenterVertically).weight(1f) // weight(1f) to push color manager to end
                    )

                }
                summaryUIElements(dbman, myColorThemeViewModel) // Box that holds the information mentioned in this row (macronutrients)
            }
        }

        // Column for all the buttons that go to the different food stuff
        Column(
            modifier = Modifier.fillMaxSize().weight(7f))
        {
            Row(modifier = Modifier.padding(4.dp)) {
                Text(
                    "Nutrition: ",
                    fontSize = 30.sp,
                    color = currentColorTheme.textColor,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                myColorDropDownMenu(myColorThemeViewModel, dbman)
            }

            // scrollable Column containing nutrition rows which represents different meal categories
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp).verticalScroll(rememberScrollState())) {
                NutritionRow(
                    R.drawable.breakfastgraphic, mealTypeText = "Breakfast", recommendedCalories = recommendedCalories, dbman,
                    onClick = {
                        navController.navigate(Screens.FOODPAGE.name)
                        currentlySelectedMeal = MealTypes.BREAKFAST
                    }
                )
                NutritionRow(R.drawable.lunchbox, mealTypeText = "Lunch", recommendedCalories = recommendedCalories, dbman,
                    onClick = {
                        navController.navigate(Screens.FOODPAGE.name)
                        currentlySelectedMeal = MealTypes.LUNCH
                    }
                )
                NutritionRow(R.drawable.dinnergraphic, mealTypeText = "Dinner", recommendedCalories = recommendedCalories, dbman,
                    onClick = {
                        navController.navigate(Screens.FOODPAGE.name)
                        currentlySelectedMeal = MealTypes.DINNER
                    }
                )
                NutritionRow(R.drawable.desertgraphic, mealTypeText = "Desert", recommendedCalories = recommendedCalories, dbman,
                    onClick = {
                        navController.navigate(Screens.FOODPAGE.name)
                        currentlySelectedMeal = MealTypes.DESERT
                    }
                )
                NutritionRow(R.drawable.snackgraphic, mealTypeText = "Snacks", recommendedCalories = recommendedCalories, dbman,
                    onClick = {
                        navController.navigate(Screens.FOODPAGE.name)
                        currentlySelectedMeal = MealTypes.SNACKS
                    }
                )
            }
        }
    }
}

// Method grabs total nutrition and meal item list from server
//@SuppressLint("MutableCollectionMutableState")
@Composable
fun GetCache(dbman: DatabaseManager, mealName: String): AddFoodPage.MealInformation {

    val totalCalories = remember { mutableStateOf("") }
    val totalCarbs = remember { mutableStateOf("") }
    val totalProtein = remember { mutableStateOf("") }
    val totalFat = remember { mutableStateOf("") }
    var items by remember { mutableStateOf(mutableListOf<ItemInfo>()) }

    val mealInformation = AddFoodPage.MealInformation(
        totalCalories = totalCalories,
        totalCarbs = totalCarbs,
        totalProtein = totalProtein,
        totalFat = totalFat,
        items = items
    )

    // calls backend method to run queries on server to get all values
    LaunchedEffect(true) {
        withContext(Dispatchers.Default) {
            val tempNutrition = dbman.getTotalMealNutrition(mealName)
            val tempItems = dbman.getMealItems(mealName)
            totalCalories.value = tempNutrition.calories
            totalCarbs.value = tempNutrition.carbs
            totalProtein.value = tempNutrition.protein
            totalFat.value = tempNutrition.fat
            items = tempItems
        }
    }
    return mealInformation
}

// Checks the database for previously logged in days, if its not the current day then it gets logged into the database
fun checkPreviousDates(dbman: DatabaseManager) {
    CoroutineScope(Dispatchers.IO).launch { // threaded
        val currentDateInstance = LocalDate.now()
        val newestDateInDatabase = dbman.getNewestDateInDatabase()
        if (currentDateInstance != newestDateInDatabase) { // its a new day if newest doesn't match so add date
            dbman.addToPastDates(currentDateInstance.dayOfMonth, currentDateInstance.monthValue, currentDateInstance.year)
            dbman.deleteAllFoodItems()
        }
    }
}

// function to cache nutritional information needed for mainScreenUI in an off main thread
fun packMealInfoList(dbman: DatabaseManager) {
    CoroutineScope(Dispatchers.IO).launch { // threaded
        val mealTypes = MealTypes.entries.toTypedArray()
        for (i in 0..<mealInfoList.size) { // default would be 5 but allows scalability
            val currentMealType = mealTypes[i]
            mealInfoList[i] = dbman.getTotalMealNutrition(currentMealType.name)
        }
    }
}

// Loads the composable that hosts the dropDownMenu to switch between dark and light theme
@Composable
fun myColorDropDownMenu(myColorThemeViewModel: ColorThemeViewModel, dbman: DatabaseManager) {

    currentColorTheme = myColorThemeViewModel.currentColorTheme
    var dropdownIsSelected by remember { mutableStateOf(false) }
    var dropdownSelectedOption by remember { mutableStateOf("Select Page") }
    val dropdownMenuOptions = listOf("Dark", "Light")

    Column(
        modifier = Modifier.size(40.dp).background(currentColorTheme.backgroundColor)
    ) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = "",
            Modifier.size(50.dp).background(currentColorTheme.backgroundColor).clickable {
                dropdownIsSelected = !dropdownIsSelected
            }
        )
        // TODO: Remove .clip from DropdownMenu modifier? Currently, it gives it white corners so it's not really clipped but looks cool still
        DropdownMenu(
            expanded = dropdownIsSelected,
            onDismissRequest = { dropdownIsSelected = false },
            modifier = Modifier.width(150.dp).background(currentColorTheme.rowColor).padding(8.dp)
        ) {
            dropdownMenuOptions.forEach { currentOption ->
                DropdownMenuItem(
                    text = { Text(currentOption, color = currentColorTheme.textColor, fontSize = 25.sp, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        // sets to selected option and deselects menu, closing it and opening the correct page composable all at once
                        dropdownSelectedOption = currentOption
                        dropdownIsSelected = false
                        when (currentOption) {
                            "Dark" -> myColorThemeViewModel.updateColorTheme(ColorTheme.DARK, dbman)
                            "Light" -> myColorThemeViewModel.updateColorTheme(ColorTheme.LIGHT, dbman)
                        }
                    }
                )
            }
        }
        currentColorTheme = myColorThemeViewModel.currentColorTheme
    }
}

// TODO: Have to pass myColorThemeViewModel here otherwise it wont update until page is returned to
@Composable
fun summaryUIElements(dbman: DatabaseManager, myColorThemeViewModel: ColorThemeViewModel) {

    val proteinRatio = 0.30
    val carbsRatio = 0.40
    val fatRatio = 0.30

    // clip here for rounded corners and better UI look
    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(myColorThemeViewModel.currentColorTheme.rowColor)) {
        // Row for calories, eaten, and burned
        Row(modifier = Modifier.fillMaxSize().weight(2f)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Total Calories:",
                    fontSize = 30.sp,
                    color = currentColorTheme.textColor,
                )
                val recommendedCalories = dbman.getRecommendedCalories()
                val dailyConsumedCalories = dbman.getTotalCalories()
                LinearProgressIndicator(
                    progress = { (1.0 * dailyConsumedCalories / recommendedCalories).toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(4.dp).height(15.dp),
                )
                // The total calories a person should eat and what they have eaten
                Text(
                    "$dailyConsumedCalories / $recommendedCalories Cal",
                    fontSize = 40.sp,
                    color = currentColorTheme.textColor,
                )
            }
        }
        // Row for Protein, Carbs, and Fat data
        Row(
            modifier = Modifier.fillMaxSize().weight(1f), // weight has to be 2 then 1 for landscape to work
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // having modifier in parameter here allows correct formatting since .weight(1f) cannot be called inside function
            summaryProgressBar("Protein", proteinRatio.toFloat(), myColorThemeViewModel, Modifier.fillMaxHeight().weight(1f), dbman)
            summaryProgressBar("Carbs", carbsRatio.toFloat(), myColorThemeViewModel, Modifier.fillMaxHeight().weight(1f), dbman)
            summaryProgressBar("Fat", fatRatio.toFloat(), myColorThemeViewModel, Modifier.fillMaxHeight().weight(1f), dbman)
            // protein: 30% diet, fat: 30% diet, carbs: 40% diet (Based on general rule of thumb from nutritionists)
        }
    }

}

// A function to load the summary box that takes up the top 2/3rds of the app
@Composable
fun summaryProgressBar(label: String, dietRatio: Float, myColorThemeViewModel: ColorThemeViewModel, modifier: Modifier = Modifier, dbman: DatabaseManager) {

    // have to set here otherwise icon wont have correct background - why...
    currentColorTheme = myColorThemeViewModel.currentColorTheme
    var macroCaloriesConsumed = 0
    val totalMacroInGrams: Int
    val totalRecommendedMacros: Int
    val progress = (macroCaloriesConsumed / (dbman.getRecommendedCalories() * dietRatio))


    if (label == "Fat") { // 9 calories per gram
        macroCaloriesConsumed = dbman.getTotalFat() * 9
        totalMacroInGrams = dbman.getTotalFat()
        totalRecommendedMacros = (dbman.getRecommendedCalories() * dietRatio).roundToInt() / 9
    } else if (label == "Protein") { // 4 calories per gram
        macroCaloriesConsumed = dbman.getTotalProtein() * 4
        totalMacroInGrams = dbman.getTotalProtein()
        totalRecommendedMacros = (dbman.getRecommendedCalories() * dietRatio).roundToInt() / 4
    } else { // 4 calories per gram for carbs
        macroCaloriesConsumed = dbman.getTotalCarbs() * 4
        totalMacroInGrams = dbman.getTotalCarbs()
        totalRecommendedMacros = (dbman.getRecommendedCalories() * dietRatio).roundToInt() / 4
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            fontSize = 20.sp,
            color = currentColorTheme.textColor,
        )

        LinearProgressIndicator(
            progress = {
                progress // Use actual fraction value (between 0-1F for 0-100%)
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp), // padding of 8 is best for landscape
        )
        // text for total grams out of grams
        Text(
            "$totalMacroInGrams / $totalRecommendedMacros grams",
            fontSize = 15.sp,
            color = currentColorTheme.textColor,
        )
        // text for calories out of total calories
        Text(
            "$macroCaloriesConsumed / ${(dbman.getRecommendedCalories() * dietRatio).roundToInt()} Cal",
            fontSize = 15.sp,
            color = currentColorTheme.textColor,
        )
    }
}

@Composable
fun myNavhostDropDownMenu(navController: NavHostController, myColorThemeViewModel: ColorThemeViewModel) {

    // have to set here otherwise icon wont have correct background - why...
    currentColorTheme = myColorThemeViewModel.currentColorTheme
    var dropdownIsSelected by remember { mutableStateOf(false) }
    var dropdownSelectedOption by remember { mutableStateOf("Select Page") }
    val dropdownMenuOptions = listOf("Profile", "Graph")

    Column(
        modifier = Modifier.size(40.dp).background(currentColorTheme.backgroundColor)
    ) {
        Icon(
            Icons.Outlined.Menu,
            contentDescription = "",
            Modifier.size(50.dp).clickable {
                dropdownIsSelected = !dropdownIsSelected
            }
                .testTag("NavhostDropdownMenuIcon")
        )
        DropdownMenu(
            expanded = dropdownIsSelected,
            onDismissRequest = { dropdownIsSelected = false },
            modifier = Modifier.width(300.dp).background(currentColorTheme.rowColor).padding(8.dp)
                .testTag("NavhostDropdownMenu")
        ) {
            dropdownMenuOptions.forEach { currentOption ->
                DropdownMenuItem(
                    text = { Text(currentOption, color = currentColorTheme.textColor, fontSize = 25.sp, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        // sets to selected option and deselects menu, closing it and opening and the correct page all at once
                        dropdownSelectedOption = currentOption
                        dropdownIsSelected = false
                        when (currentOption) {
                            "Profile" -> navController.navigate(Screens.PROFILEPAGE.name)
                            "Graph" -> navController.navigate(Screens.GRAPHPAGE.name)
                        }
                    }
                )
            }
        }

    }
}

// onClick in NutritionRow's parameter is used to simplify navController usage when nutrition data is added
@Composable
fun NutritionRow(imageID: Int, mealTypeText: String, recommendedCalories: Int, dbman: DatabaseManager, onClick: () -> Unit) {

    // Spacer at start instead of end so no extra space at bottom and some extra space after 'Nutrition:'
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(currentColorTheme.rowColor)
    ) {
        // Images that is displayed in the nutrition row
        Image(
            painter = painterResource(id = imageID),
            contentDescription = "Row Image for nutrition data",
            modifier = Modifier.size(100.dp).padding(4.dp).clip(RoundedCornerShape(16.dp)).align(Alignment.CenterVertically),
            contentScale = ContentScale.Crop
        )
        // using weight(1f) here has the text take up all the space moving button to far right perfectly each time
        Column(modifier = Modifier.align(Alignment.CenterVertically).weight(1f).padding(4.dp)) {
            Text(mealTypeText,
                fontSize = 25.sp,
                color = currentColorTheme.textColor,
                fontWeight = FontWeight.Bold
            )

            // Text holding total calories against total of each meal category
            Text("${dbman.getTotalMealNutrition(mealTypeText).calories} / $recommendedCalories Cal",
                fontSize = 20.sp,
                color = currentColorTheme.textColor,
            )
        }
        // Button to go to the add food page
        Button(
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterVertically).padding(8.dp)
                .testTag("${mealTypeText}-Button")
        ) {
            Text(text = "+",
                fontSize = 20.sp,
                color = currentColorTheme.textColor
            )
        }
    }
}

fun correctFoodCapitalization(input: String): String {
    return if (input.isNotEmpty()) {
        input[0].uppercase() + input.substring(1).lowercase() // sets first char to upper and rest to lower
    } else {
        input // return empty (null)
    }
}
