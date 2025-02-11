package com.example.calorie_tracker

import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.util.TypedValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import android.content.res.Resources
import androidx.annotation.Dimension
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WeightDate(
    var weight: Int,
    var date: String
)

class viewGraphPage {
    @Composable
    fun ComplexGraphController(navController: NavHostController, myColorThemeViewModel: ColorThemeViewModel, dbman: DatabaseManager){

        var lstUserWeight = dbman.getAllWeight().toSet().toList()
        var  targetWeight = dbman.getCurrentTargetWeight()

        val dailyLst = getDailyWeightList(lstUserWeight)
        val weeklyLst = getWeeklyWeightList(lstUserWeight)
        val yearlyLst = getYearlyWeightList(lstUserWeight)

        val dailyLstWeight = getWeights(dailyLst)
        val dailyLstDate = getDates(dailyLst, 7)
        val dailyLstSize by remember { mutableStateOf(dailyLstWeight.size) }

        val weeklyLstWeight = getWeights(weeklyLst)
        val weeklyLstDate = getDates(weeklyLst, 5)
        val weeklyLstSize by remember { mutableStateOf(weeklyLstWeight.size) }

        val yearlyLstWeight = getWeights(yearlyLst)
        val yearlyLstDate = getDatesYear(yearlyLst, 12)
        val yearlyLstSize by remember { mutableStateOf(yearlyLstWeight.size) }


        Log.d("@@@", "--------- New list ---------")
        Log.d("@@@", "list: ${weeklyLstWeight.joinToString(", ")}")

        var startingWeight: String = "Starting Weight: "
        val targetWeightFormat: String = "Goal Weight: $targetWeight kg"
        var currWeight: String = "Current: "
        var diff: String = "Difference: "

        if (lstUserWeight.size > 0){
            var weightInfo = lstUserWeight.get(0)
            startingWeight = "Starting Weight: ${weightInfo.weight} kg"
            weightInfo = lstUserWeight.get(lstUserWeight.size-1)
            currWeight = "Current: ${weightInfo.weight} kg"
            val diffAmt = targetWeight - weightInfo.weight
            if (diffAmt > 0){
                diff = "Difference: +$diffAmt kg"
            } else {
                diff = "Difference: $diffAmt kg"
            }
        }

        val gridSize = 7

        var tabIndex = rememberSaveable { mutableStateOf(0) }
        val tabs = listOf("Daily", "Weekly", "Yearly")

        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "",
                        Modifier
                            .size(50.dp)
                            .clickable {
                                navController.navigate(Screens.MAINSCREEN.name)
                            }
                    )
                    Text(
                        text = "Graph",
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            containerColor = myColorThemeViewModel.currentColorTheme.backgroundColor,
            contentColor = myColorThemeViewModel.currentColorTheme.textColor

        ) { innerPadding ->
            if (Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                GraphInfoLandscape(
                    innerPadding,
                    gridSize,
                    tabIndex,
                    tabs,
                    dailyLstWeight,
                    weeklyLstWeight,
                    yearlyLstWeight,
                    dailyLstSize,
                    weeklyLstSize,
                    yearlyLstSize,
                    dailyLstDate,
                    weeklyLstDate,
                    yearlyLstDate,
                    startingWeight,
                    targetWeightFormat,
                    currWeight,
                    diff
                )
            } else {
                GraphInfo(
                    innerPadding,
                    gridSize,
                    tabIndex,
                    tabs,
                    dailyLstWeight,
                    weeklyLstWeight,
                    yearlyLstWeight,
                    dailyLstSize,
                    weeklyLstSize,
                    yearlyLstSize,
                    dailyLstDate,
                    weeklyLstDate,
                    yearlyLstDate,
                    startingWeight,
                    targetWeightFormat,
                    currWeight,
                    diff
                )
            }

        }
    }

    @Composable
    fun GraphInfo (
        innerPadding: PaddingValues,
        gridSize: Int,
        tabI: MutableState<Int>,
        tabs: List<String>,
        lst1: List<Int>,
        lst2: List<Int>,
        lst3: List<Int>,
        lstSize1:  Int,
        lstSize2:  Int,
        lstSize3:  Int,
        lstOfDates1: List<String>,
        lstOfDates2: List<String>,
        lstOfDates3: List<String>,
        startingWeight: String,
        targetWeightFormat: String,
        currWeight: String,
        diff: String
    ) {
        var tabIndex by tabI
        val configuration = LocalConfiguration.current
        val screenDimension = configuration.screenWidthDp.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                Modifier.align(Alignment.TopCenter)
            ) {
                TabRow(selectedTabIndex = tabIndex,
                    containerColor = Color(0xff2b3240),
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .clip(RoundedCornerShape(15)),
                    indicator = { tabPositions: List<TabPosition> ->
                        Box {}
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, text ->
                        val selected = tabIndex == index
                        Tab(
                            modifier = if (selected)
                                Modifier
                                    .clip(RoundedCornerShape(15))
                                    .background(Color(0xff616875))
                            else
                                Modifier
                                    .clip(RoundedCornerShape(15))
                                    .background(Color(0xff2b3240)),
                            selected = selected,
                            onClick = { tabIndex = index },
                            text = {
                                Text(
                                    text = text,
                                    color = Color.White
                                )
                            }
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(10.dp, 25.dp, 0.dp, 25.dp),
                ) {
                    Text(
                        "\u2022  $startingWeight",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 7.dp),
                    )
                    Text(
                        "\u2022  $targetWeightFormat",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 7.dp),
                    )
                    Text(
                        "\u2022  $currWeight",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 7.dp),
                    )
                    Text(
                        "\u2022  $diff",
                        fontSize = 25.sp
                    )
                }
                when (tabIndex) {
                    0 -> if (lstSize1 >= 2){
                        ComplexGraphUI(lst1, gridSize, lstOfDates1, screenDimension)
                    } else {
                        Text(
                            "Not enough information to draw graph",
                            textAlign = TextAlign.Center,
                            fontSize = 30.sp
                        )
                    }
                    1 -> if (lstSize2 >= 2){
                        ComplexGraphUI(lst2, 5, lstOfDates2, screenDimension)
                    } else {
                        Text(
                            "Not enough information to draw graph",
                            textAlign = TextAlign.Center,
                            fontSize = 30.sp
                        )
                    }
                    2 -> if (lstSize3 >= 2){
                        ComplexGraphUI(lst3, 12, lstOfDates3, screenDimension)
                    } else {
                        Text(
                            "Not enough information to draw graph",
                            textAlign = TextAlign.Center,
                            fontSize = 30.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun GraphInfoLandscape (
        innerPadding: PaddingValues,
        gridSize: Int,
        tabI: MutableState<Int>,
        tabs: List<String>,
        lst1: List<Int>,
        lst2: List<Int>,
        lst3: List<Int>,
        lstSize1:  Int,
        lstSize2:  Int,
        lstSize3:  Int,
        lstOfDates1: List<String>,
        lstOfDates2: List<String>,
        lstOfDates3: List<String>,
        startingWeight: String,
        targetWeightFormat: String,
        currWeight: String,
        diff: String
    ) {
        var tabIndex by tabI
        val configuration = LocalConfiguration.current
        val screenDimension = configuration.screenHeightDp.dp
        val density = LocalDensity.current
        var sizeInDp by remember { mutableStateOf(DpSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                Modifier.align(Alignment.TopCenter)
            ) {

                Row(
                    modifier = Modifier.fillMaxHeight()
                        .onSizeChanged {
                            sizeInDp = density.run {
                                DpSize(
                                    it.width.toDp(),
                                    it.height.toDp()
                                )
                            }
                        }
                ) {
                    Column(
//                        modifier = Modifier.padding(10.dp, 25.dp, 25.dp, 25.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TabRow(selectedTabIndex = tabIndex,
                            containerColor = Color(0xff2b3240),
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .clip(RoundedCornerShape(15)),
                            indicator = { tabPositions: List<TabPosition> ->
                                Box {}
                            },
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, text ->
                                val selected = tabIndex == index
                                Tab(
                                    modifier = if (selected)
                                        Modifier
                                            .clip(RoundedCornerShape(15))
                                            .background(Color(0xff616875))
                                    else
                                        Modifier
                                            .clip(RoundedCornerShape(15))
                                            .background(Color(0xff2b3240)),
                                    selected = selected,
                                    onClick = { tabIndex = index },
                                    text = {
                                        Text(
                                            text = text,
                                            color = Color.White
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            "\u2022  $startingWeight",
                            fontSize = 25.sp,
                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 7.dp),
                        )
                        Text(
                            "\u2022  $targetWeightFormat",
                            fontSize = 25.sp,
                            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 7.dp),
                        )
                        Text(
                            "\u2022  $currWeight",
                            fontSize = 25.sp,
                            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 7.dp),
                        )
                        Text(
                            "\u2022  $diff",
                            fontSize = 25.sp
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (tabIndex) {
                            0 -> if (lstSize1 >= 2) {
                                ComplexGraphUI(lst1, gridSize, lstOfDates1, sizeInDp.height)
                            } else {
                                Text(
                                    "Not enough information to draw graph",
                                    textAlign = TextAlign.Center,
                                    fontSize = 30.sp
                                )
                            }

                            1 -> if (lstSize2 >= 2) {
                                ComplexGraphUI(lst2, 5, lstOfDates2, sizeInDp.height)
                            } else {
                                Text(
                                    "Not enough information to draw graph",
                                    textAlign = TextAlign.Center,
                                    fontSize = 30.sp
                                )
                            }

                            2 -> if (lstSize3 >= 2) {
                                ComplexGraphUI(lst3, 12, lstOfDates3, sizeInDp.height)
                            } else {
                                Text(
                                    "Not enough information to draw graph",
                                    textAlign = TextAlign.Center,
                                    fontSize = 30.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }



    @Composable
    fun ComplexGraphUI(lstOfWghtPerDay: List<Int>, gridSize: Int, lstOfDates: List<String>, screenDimension: Dp){
        val srtdLstofWghtPerDay = lstOfWghtPerDay.sorted()
        val incrementAmount = NumberIncremented(lstOfWghtPerDay, srtdLstofWghtPerDay)
        val columLabelList = unitToColumLabel(srtdLstofWghtPerDay, incrementAmount, gridSize)
        val caloriesInAWeek = unitToGridComplex(lstOfWghtPerDay, columLabelList)
        val canvasSize =  (screenDimension) - 50.dp

        Log.d("@@@", "--------- New Refresh ---------")
        Log.d("@@@", "list: ${caloriesInAWeek.joinToString(", ")}")
        Log.d("@@@", "Max size: ${Modifier.fillMaxSize()}")

        Row(

        ) {
            /* Prints labels for Y-axis */
            Column(
                Modifier.height(canvasSize),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = " ",
                )
                for (i in 0 until columLabelList.size) {
                    Text(
                        text = "${columLabelList.get((columLabelList.size-1)-i)}",
                        modifier = Modifier.padding(0.dp, 0.dp, 5.dp, 0.dp),
                        fontSize = 10.sp

                    )
                }
            }
            Column (

            ) {
                Canvas(modifier = Modifier.size(canvasSize)) { // Modifier.size(200.dp)
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val numOfGrids = gridSize
                    val yAxisGridLine = canvasHeight/numOfGrids
                    val xAxisGridLine = canvasWidth/numOfGrids
                    var lineCoordinates: Float
                    val xCoordinates = mutableListOf<Float>()
                    val yCoordinates = mutableListOf<Float>()

                    Log.d("@@@@", "canvasSize: ${canvasHeight}")

                    // when generating grid only does 1 through numOfGrids so need to add 0 here
                    xCoordinates.add(0f)

                    /* -------- Draw graph boarder: X-axis -------- */
                    drawLine(
                        start = Offset(x = 0f, y = canvasHeight),
                        end = Offset(x = canvasWidth, y = canvasHeight),
                        color = Color.Black,
                        strokeWidth = 10f
                    )
                    /* -------- Draw graph boarder: Y-axis -------- */
                    drawLine(
                        start = Offset(x = 0f, y = 0f),
                        end = Offset(x = 0f, y = canvasHeight),
                        color = Color.Black,
                        strokeWidth = 10f
                    )

                    /* -------- Draw grid cells: Y-axis (note, Y-axis is inverted) -------- */
                    for (i in 1 until numOfGrids){
                        lineCoordinates = yAxisGridLine*i
                        yCoordinates.add(lineCoordinates)
                        drawLine(
                            start = Offset(x = lineCoordinates, y = 0f),
                            end = Offset(x = lineCoordinates, y = canvasHeight),
                            color = Color.LightGray,
                            strokeWidth = 5f
                        )
                    }
                    /* Since Y-axis is inverted this is effective adding the 0 like in the
                       X-axis above */
                    lineCoordinates = yAxisGridLine*numOfGrids
                    yCoordinates.add(lineCoordinates)

                    /* -------- Draw grid cells: X-axis -------- */
                    for (i in 1 until numOfGrids){
                        lineCoordinates = xAxisGridLine*i
                        xCoordinates.add(lineCoordinates)
                        drawLine(
                            start = Offset(x = canvasWidth, y = lineCoordinates ),
                            end = Offset(x = 0f, y = lineCoordinates),
                            color = Color.LightGray,
                            strokeWidth = 5f
                        )
                    }

                    /* -------- Draw plot line -------- */
                    // used to get y coordinate placement since yCoordinates is inverted
                    val yCoordinatesSize = yCoordinates.size-1
                    val currentIndexYCoordinate = yCoordinates.get(0)
                    val smallestValInYAxisList = srtdLstofWghtPerDay.get(0)
                    // if graph is counting by 6 it would take 6 preciseCoordinatePointYAxis to reach the next Y-axis label
                    val preciseCoordinatePointYAxis = currentIndexYCoordinate/(incrementAmount)


                    Log.d("@@@", "list of X: ${xCoordinates.joinToString(", ")}")
                    Log.d("@@@", "list of Y: ${yCoordinates.joinToString(", ")}")

                    for (i in 0 until caloriesInAWeek.size-1) {
                        val currentYCoordinateIndex = yCoordinatesSize-caloriesInAWeek.get(i)
                        val currentYCoordinateNextIndex = yCoordinatesSize-caloriesInAWeek.get(i+1)

                        // gets coordinate placement between Y-axis column labels
                        // done by getting remainder of value from label val minus smallest val
                        // then getting the amount of precise coordinate points for specific index
                        val remainder: Int
                        val remainderNextIndex: Int
                        if (incrementAmount == 0) {
                            remainder = 0
                            remainderNextIndex = 0
                        } else {
                            remainder = (lstOfWghtPerDay.get(i) - smallestValInYAxisList) % incrementAmount
                            remainderNextIndex = (lstOfWghtPerDay.get(i+1) - smallestValInYAxisList) % incrementAmount
                        }
                        var preciseCoordinate = (preciseCoordinatePointYAxis)*((incrementAmount)-remainder)
                        var preciseCoordinateNextIndex = (preciseCoordinatePointYAxis)*((incrementAmount)-remainderNextIndex)

                        // if remainder is zero sets the preciseCoordinate its current label instead of the next one
                        Log.d("@@@", "yCoordinates max: ${yCoordinates.maxOf {it}}")
                        Log.d("@@@", "currentIndexYCoordinate: $currentIndexYCoordinate")

                        if (yCoordinates.maxOf {it} == yCoordinates.get(currentYCoordinateIndex)) {
                            Log.d("@@@", "entered")
                            preciseCoordinate = 0f
                        }
                        if (yCoordinates.maxOf {it} == yCoordinates.get(currentYCoordinateNextIndex)) {
                            preciseCoordinateNextIndex = 0f
                        }

                        drawLine(
                            start = Offset(x = xCoordinates.get(i), y = yCoordinates.get(currentYCoordinateIndex) + preciseCoordinate),
                            end = Offset(x = xCoordinates.get(i+1), y = yCoordinates.get(currentYCoordinateNextIndex) + preciseCoordinateNextIndex),
                            color = Color.Red,
                            strokeWidth = 5f
                        )
                    }
                }

                /* Prints labels for X-axis */
                Row(
                    modifier = Modifier.width(canvasSize),
                    horizontalArrangement = Arrangement.SpaceBetween

                ) {
                    for (i in 0 until lstOfDates.size) {
                        Text(
                            text = lstOfDates.get(i),
                            modifier = Modifier
                                .rotate(75f)
                                .padding(0.dp, 30.dp, 0.dp, 0.dp),
//                                    .weight(1f),
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = " ",
//                            modifier = Modifier.weight(1f)
                    )


                }
            }

        }
    }
    companion object {
        /*
         *  Description: gets a list of the nearest label value for unit value of graph.
         *  When unit value is added to list it must be less than label value.
         */
        fun unitToGridComplex(unitList: List<Int>, columLabelList: List<Int>): List<Int> {
            val unitGridFormat =  mutableListOf<Int>()
            val columLabelLst = columLabelList.toSet().toMutableList()

            for (i in 0 until unitList.size) {
                for (j in 0 until columLabelLst.size) {
                    if (unitList.get(i) == columLabelLst.get(0)){
                        unitGridFormat.add(j)
                        break;
                    } else if (unitList.get(i) < columLabelLst.get(j) ) {
                        unitGridFormat.add(j)
                        break;
                    }
                }
            }

            return unitGridFormat

        }

        /*
         * Description: returns what the column label should increment by based on
         * a range data that does not increment evenly.
         * example: the Y-axis label goes 0, 2, 4, 6
         */
        fun NumberIncremented(unitList: List<Int>, sortedUnits: List<Int>): Int {
            var maxMinDiff = sortedUnits.get(sortedUnits.size - 1) - sortedUnits.get(0)

            for (i in 1 until unitList.size) {
                if (maxMinDiff % (unitList.size - 1) != 0) {
                    maxMinDiff++
                }
            }

            return maxMinDiff / (unitList.size - 1)
        }


        /*
         * Description: creates the labels for the Y-axis graph based on
         * incremented value.
         */
        fun unitToColumLabel(sortedUnits: List<Int>, numbIncrement: Int, gridSize: Int): List<Int> {
            val unitColumnFormat = mutableListOf<Int>()
            var numInc = numbIncrement

            unitColumnFormat.add(sortedUnits.get(0))
            if (numbIncrement == 0) {
                numInc = 1
            }
            val idx = sortedUnits.get(0)
            for (i in 1 until gridSize) {
                unitColumnFormat.add(idx + (numInc * i))
            }

            return unitColumnFormat
        }

        fun getDailyWeightList(lstUserWeight: List<WeightInfo>): List<WeightDate> {
            val lstOfDailyWeight = mutableListOf<WeightDate>()
            var weight: WeightInfo
            if (lstUserWeight.size < 7){
                for (i in 0 until lstUserWeight.size) {
                    weight = lstUserWeight.get(i)
                    lstOfDailyWeight.add(WeightDate(
                        weight.weight.toInt(),
                        dateToString(
                            weight.year,
                            weight.month,
                            weight.Day
                        )
                    ))
                }
            } else {
                for (i in lstUserWeight.size-7 until lstUserWeight.size) {
                    weight = lstUserWeight.get(i)
                    lstOfDailyWeight.add(WeightDate(
                        weight.weight.toInt(),
                        dateToString(
                            weight.year,
                            weight.month,
                            weight.Day
                        )
                    ))
                }
            }
            return lstOfDailyWeight
        }

        fun getWeeklyWeightList(lstUserWeight: List<WeightInfo>): List<WeightDate> {
            val lstOfWeeklyWeight = mutableListOf<WeightDate>()
            var weight: WeightInfo
            var userLstSize = lstUserWeight.size
            var weightAvg = 0
            var weekCont = 0
            var currWeek = 0
            var nextWeek = 7
            if (userLstSize < 5*7){
                while (weekCont < 5) {
                    if (userLstSize >= 7) {
                        for (i in currWeek until nextWeek) {
                            weight = lstUserWeight.get(i)
                            weightAvg += weight.weight.toInt()
                        }
                        weightAvg /= 7
                        weight = lstUserWeight.get(nextWeek-1)
                        lstOfWeeklyWeight.add(WeightDate(
                            weightAvg,
                            dateToString(
                                weight.year,
                                weight.month,
                                weight.Day
                            )
                        ))
                        currWeek += 7
                        nextWeek += 7
                        userLstSize -= 7
                        weekCont++
                        weightAvg = 0
                    } else {
                        break
                    }
                }
            } else {
                currWeek = userLstSize - 5*7
                nextWeek = currWeek + 7
                while (weekCont < 5) {
                    if (userLstSize >= 7) {
                        for (i in currWeek until nextWeek) {
                            weight = lstUserWeight.get(i)
                            weightAvg += weight.weight.toInt()
                        }
                        weightAvg /= 7
                        weight = lstUserWeight.get(nextWeek-1)
                        lstOfWeeklyWeight.add(WeightDate(
                            weightAvg,
                            dateToString(
                                weight.year,
                                weight.month,
                                weight.Day
                            )
                        ))
                        currWeek += 7
                        nextWeek += 7
                        userLstSize -= 7
                        weekCont++
                        weightAvg = 0
                    } else {
                        break
                    }
                }
            }

            return lstOfWeeklyWeight
        }

        fun getYearlyWeightList(lstUserWeight: List<WeightInfo>): List<WeightDate> {
            val lstOfYearlyWeight = mutableListOf<WeightDate>()
            val userLstSize = lstUserWeight.size
            var weight: WeightInfo
            var currMonth: Int
            var weightAvg = 0
            var avgAmt = 0

            if (userLstSize == 0){
                return lstOfYearlyWeight
            }
            weight = lstUserWeight.get(0)
            weightAvg += weight.weight.toInt()
            avgAmt++
            currMonth = weight.month
            for (i in 1 until userLstSize) {
                weight = lstUserWeight.get(i)
                if (currMonth == weight.month) {
                    weightAvg += weight.weight.toInt()
                    avgAmt++;
                } else {

                    if (avgAmt == 0) {
                        avgAmt = 1
                    }
                    weightAvg /= avgAmt

                    if (weightAvg == 0){
                        lstOfYearlyWeight.add(WeightDate(weight.weight.toInt(), dateToString(weight.year, currMonth)))
                    } else {
                        lstOfYearlyWeight.add(WeightDate(weightAvg, dateToString(weight.year, currMonth)))
                    }

                    avgAmt = 0
                    weightAvg = 0
                    weightAvg += weight.weight.toInt()
                    avgAmt++
                }
                currMonth = weight.month
            }
            if (avgAmt == 0){
                avgAmt = 1
            }

            weightAvg /= avgAmt

            lstOfYearlyWeight.add(WeightDate(weightAvg, dateToString(weight.year, weight.month)))

            return lstOfYearlyWeight
        }

        fun dateToString(year: Int, month: Int, day: Int): String{
            val date = "$day/$month/$year"
            return date
        }

        fun dateToString(year: Int, month: Int): String{
            val yearFormat = year
            val date = "$month/$yearFormat"
            return date
        }

        fun getWeights(lstOfWeightInfo: List<WeightDate>): List<Int> {
            val lstOfWeight = mutableListOf<Int>()
            for (i in 0 until lstOfWeightInfo.size) {
                lstOfWeight.add(lstOfWeightInfo.get(i).weight)
            }
            return lstOfWeight
        }

        fun getDates(lstOfWeightInfo: List<WeightDate>, lenNeeded: Int): List<String> {
            val lstOfDates = mutableListOf<String>()
            for (i in 0 until lstOfWeightInfo.size) {
                lstOfDates.add(lstOfWeightInfo.get(i).date)
            }
            if (lstOfWeightInfo.size < lenNeeded) {
                for (i in lstOfWeightInfo.size until lenNeeded) {
                    lstOfDates.add("0/0/0000")
                }
            }

            return lstOfDates
        }

        fun getDatesYear(lstOfWeightInfo: List<WeightDate>, lenNeeded: Int): List<String> {
            val lstOfDates = mutableListOf<String>()
            for (i in 0 until lstOfWeightInfo.size) {
                lstOfDates.add(lstOfWeightInfo.get(i).date)
            }
            if (lstOfWeightInfo.size < lenNeeded) {
                for (i in lstOfWeightInfo.size until lenNeeded) {
                    lstOfDates.add("00/00")
                }
            }
            return lstOfDates
        }
    }
}



