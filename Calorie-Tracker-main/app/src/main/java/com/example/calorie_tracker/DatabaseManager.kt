package com.example.calorie_tracker


import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalGraphicsContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate


data class TotalMealInfo(
    var calories: String,
    var protein: String,
    var carbs: String,
    var fat: String
)

data class ItemInfo(
    var description: String,
    var calories: String,
    var protein: String,
    var carbs: String,
    var fat: String
)

data class WeightInfo(
    var Day: Int,
    var month: Int,
    var year: Int,
    var weight: Double
)



open class DatabaseManager(context: Context) : SQLiteOpenHelper(context, "MyDb",
    null, 1) {
    // This function is only called once! on database creation not activity creation
    override fun onCreate(db: SQLiteDatabase?) {
        // Alex: Create Table to store all meal information
        db?.execSQL("CREATE TABLE IF NOT EXISTS MEALS(mealName TEXT, foodName Text, calories INT, protein INT, carbs INT, fat INT)")


        // Aidan: Created Table for UserData
        db?.execSQL("CREATE TABLE IF NOT EXISTS USERPROFILE(name TEXT, emailAddress TEXT, age INT, weight DOUBLE, inchHeight DOUBLE, gender INT, recommendedCalories INT)")

        // Aidan: Created Table for StoredAppInformation between log ins
        db?.execSQL("CREATE TABLE IF NOT EXISTS STOREDAPPINFORMATION(targetWeight INT, colorTheme INT)")

        // Aidan: Created Table for all past logiin dates
        db?.execSQL("CREATE TABLE IF NOT EXISTS PASTDATES(dateDay INT, dateMonth Int, dateYear Int, targetWeightAtTime INT)")
    }


    // used for updating database with new version
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    fun checkForExistingUserData(nameRememberable: MutableState<String>, emailRememberable: MutableState<String>, ageRememberable: MutableState<String>, weightRememberable: MutableState<String>, inchHeightRememberable: MutableState<String>) {
        val cursor = readableDatabase.rawQuery("SELECT * FROM USERPROFILE", null)

        while(cursor.moveToNext()) { // if the single row is in the database it will grab in here
            nameRememberable.value = cursor.getString(0)
            emailRememberable.value = cursor.getString(1)
            ageRememberable.value = cursor.getInt(2).toString()
            weightRememberable.value = cursor.getDouble(3).toString()
            inchHeightRememberable.value = cursor.getDouble(4).toString()
        }

        cursor.close()
    }


    // ! Using writable database below is the same as calling a db:SQLiteDatabase within dbManager
    fun clearUserProfile() {
        writableDatabase.execSQL("DELETE FROM USERPROFILE")
    }

    fun insertUserProfileData(data: String) {
        writableDatabase.execSQL("INSERT INTO USERPROFILE(name, emailAddress, age, weight, inchHeight, gender, recommendedCalories) VALUES ($data)")
    }

    fun clearStoredAppData() {
        writableDatabase.execSQL("DELETE FROM STOREDAPPINFORMATION")
    }

    fun insertUpdatedWeight(targetWeight: Int, currentColorTheme: ColorTheme) {
        writableDatabase.execSQL("INSERT INTO STOREDAPPINFORMATION(targetWeight, colorTheme) VALUES ($targetWeight, ${currentColorTheme.ordinal})")
    }


    fun addToPastDates(dateDay: Int, dateMonth: Int, dateYear: Int) {
        writableDatabase.execSQL("INSERT INTO PASTDATES(dateDay, dateMonth, dateYear, targetWeightAtTime) VALUES ($dateDay, $dateMonth, $dateYear, ${getCurrentTargetWeight()})")
    }

    fun addToPastDatesTesting(dateDay: Int, dateMonth: Int, dateYear: Int, weight: Double) {
        writableDatabase.execSQL("INSERT INTO PASTDATES(dateDay, dateMonth, dateYear, targetWeightAtTime) VALUES ($dateDay, $dateMonth, $dateYear, ${weight})")
    }

    fun getNewestDateInDatabase(): LocalDate {
        var newestDate = LocalDate.now()

        val cursor = readableDatabase.rawQuery("SELECT * FROM PASTDATES", null)
        var tableHasData = false
        while (cursor.moveToNext()) {

            // only want to add if not equal to newestDate, IE: current date is not last dat signed in
            if (cursor.isLast) {
                Log.d("$$$$$$$", "Newest date is : ${cursor.getInt(0)} / ${cursor.getInt(1)} / ${cursor.getInt(2)}")

//                val day = cursor.getInt(0)
//                val month = cursor.getInt(1)
//                val year = cursor.getInt(2)
//
//                Log.d("$$$$$$$", "variables are: $day, $month, $year")
////                val newestDate = LocalDate.of(year, month, day)
//
////                return newestDate // return newest date if found in database

                Log.d("$$$$$$$", "Set return date to $newestDate")

                tableHasData = true
                newestDate = LocalDate.of(cursor.getInt(2), cursor.getInt(1), cursor.getInt(0))  // LocalDate constructor takes (year, month, day)
            }

        }

        // If table is completely empty from newestDate being current date, then add first entry to table
        if (!tableHasData) { // if false
            Log.d("@@@", "Adding first entry to date database ${newestDate.toString()}")
            addToPastDates(newestDate.dayOfMonth, newestDate.monthValue, newestDate.year)
        }

        cursor.close() // closes for the rawQuery error
        return newestDate
//        return LocalDate.now() // return current date if empty database
    }

    fun getCurrentTargetWeight(): Int {
        var currentTargetWeight = 0
        val cursor = readableDatabase.rawQuery("SELECT * FROM STOREDAPPINFORMATION", null)

        while (cursor.moveToNext()) { // if database has the single entry get it
            currentTargetWeight = cursor.getInt(0) // targetWeight in first poisition
        }

        cursor.close() // closes for the rawQuery error
        return currentTargetWeight
    }

    fun getAllWeight(): List<WeightInfo> {
//            var totalProtein = 0
        val lstOfWeights = mutableListOf<WeightInfo>()
        var weightInfo: WeightInfo
        var year: Int
        var month: Int
        var Day: Int
        var weight: Double

        val cursor = readableDatabase.rawQuery("SELECT * FROM PASTDATES", null)

        while (cursor.moveToNext()) {
//                totalProtein += cursor.getInt(3) // protein stored in third column
            if (cursor.getDouble(3).toInt() != 0){
                weightInfo = WeightInfo(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getDouble(3))
                lstOfWeights.add(weightInfo)
            }

        }

        cursor.close() // closes for the rawQuery error

        return  lstOfWeights
    }

    fun checkIfExistingColorTheme(): Int {
        var desiredColorTheme = -1; // check against -1 in mainUI

        val cursor = readableDatabase.rawQuery("SELECT * FROM STOREDAPPINFORMATION", null)

        if (cursor.moveToFirst()) { // an existing row in the table meaning a desired colorTheme is available, otherwise set to DARK
            desiredColorTheme = cursor.getInt(cursor.getColumnIndexOrThrow("colorTheme"))
        }

        cursor.close()
        return desiredColorTheme
    }


    // Insert a food item into the database FoodEntry defined in AddFoodPage.kt
    fun insertFood(foodEntry: AddFoodPage.FoodEntry) {
        writableDatabase.execSQL("INSERT INTO MEALS VALUES(\"${foodEntry.meal}\", \"${foodEntry.description}\", " +
                "${foodEntry.calories}, ${foodEntry.protein}, ${foodEntry.carbs}, ${foodEntry.fat})")
    }

    fun deleteFoodItem(foodEntry: AddFoodPage.FoodEntry) {
        Log.d("delete", foodEntry.toString())
        Log.d("delete", getMealItems(foodEntry.meal).toString())
        writableDatabase.execSQL("DELETE FROM MEALS WHERE mealName = ? AND foodName = ? AND  calories = ? AND protein = ? AND carbs = ? AND fat = ?",
            arrayOf(foodEntry.meal, foodEntry.description, foodEntry.calories, foodEntry.protein, foodEntry.carbs, foodEntry.fat)
        )
    }

    fun deleteAllFoodItems() {
        writableDatabase.execSQL("DELETE FROM MEALS")
    }

    fun updateColorThemeInDB(colorTheme: Int) {

        val cursor = readableDatabase.rawQuery("SELECT * FROM STOREDAPPINFORMATION", null)

        if (cursor.moveToNext()) {
            writableDatabase.execSQL("UPDATE STOREDAPPINFORMATION SET colorTheme = $colorTheme")
        } else {
            writableDatabase.execSQL("INSERT INTO STOREDAPPINFORMATION(targetWeight, colorTheme) VALUES (null, $colorTheme)")
        }

        cursor.close()
    }


    // !!! Below are MEALS related table functions

    // pass in a mealName and this will return the associated meals total calories and total macronutrients
    // provided in the data class TotalMealInfo
    // mealName: database has the following values: breakfast, lunch, dinner, desert, snack
    @SuppressLint("Recycle")    // TODO: Add error handling
    fun getTotalMealNutrition(mealName: String): TotalMealInfo {

        var calories = 0
        var protein = 0
        var carbs = 0
        var fat = 0

        val info = TotalMealInfo(
            calories = "0",
            protein = "0",
            carbs = "0",
            fat = "0"
        )

        // query returns all meal items from specified meal
        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS WHERE mealName = ?",
            arrayOf(mealName)
        )
        // iterate through meal items retrieving and accumulating macro nutrition
        while (cursor.moveToNext()) {
            calories += cursor.getInt(2)
            protein += cursor.getInt(3)
            carbs += cursor.getInt(4)
            fat += cursor.getInt(5)
        }

        info.calories = calories.toString()
        info.protein = protein.toString()
        info.carbs = carbs.toString()
        info.fat = fat.toString()
        cursor.close()

        return info
    }

    fun getMealItems(mealName: String): MutableList<ItemInfo> {
        var items: MutableList<ItemInfo> = ArrayList()

        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS WHERE mealName = ?",
            arrayOf(mealName)
        )

        while (cursor.moveToNext()) {
            val info = ItemInfo(
                description = cursor.getString(1),
                calories = cursor.getInt(2).toString(),
                protein = cursor.getInt(3).toString(),
                carbs = cursor.getInt(4).toString(),
                fat = cursor.getInt(5).toString()
            )
            items.add(info)
        }
        Log.d("@@@", items.toString())

        return items
    }

    fun getTotalCalories():Int {
        var totalCalories = 0

        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS", null)

        while (cursor.moveToNext()) {
            totalCalories += cursor.getInt(2) // calories stored in second column
        }

        cursor.close() // closes for the rawQuery error
        return totalCalories
    }

    fun getTotalProtein():Int {
        var totalProtein = 0

        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS", null)

        while (cursor.moveToNext()) {
            totalProtein += cursor.getInt(3) // protein stored in third column
        }

        cursor.close() // closes for the rawQuery error
        return totalProtein
    }

    fun getTotalCarbs():Int {
        var totalCarbs = 0

        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS", null)

        while (cursor.moveToNext()) {
            totalCarbs += cursor.getInt(4) // carbs stored in fourth column
        }

        cursor.close() // closes for the rawQuery error
        return totalCarbs
    }

    fun getTotalFat():Int {
        var totalFat = 0

        val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS", null)

        while (cursor.moveToNext()) {
            totalFat += cursor.getInt(5) // fat stored in fifth column
        }

        cursor.close() // closes for the rawQuery error
        return totalFat
    }

    fun getRecommendedCalories(): Int {
        val cursor = readableDatabase.rawQuery("SELECT * FROM USERPROFILE", null)
        var recommendCaloriesReturn = 2000;

        if (cursor.moveToNext()) { // if stored in database then access and return instead
            recommendCaloriesReturn = cursor.getInt(6) // column 6 stores recommendedCalories value
        }

        cursor.close()
        return recommendCaloriesReturn
    }



    // Below nested class is used with the SQL database in order to user data permanently on the phone until
    // the app is accessed again at a later date. This allows actual long term storage of userData
    class userDataDM(context: Context): DatabaseManager(context) {

        // Sample code on inserting data to database: I will change later
        fun insertUserData(myUserData: UserData) {
            writableDatabase.execSQL("INSERT INTO USERPROFILE VALUES(myUserData.name, myUserData.emailAddress, myUserData.age, myUserData.weigt, myUserData.inchHeight)")
        }

        // Get the first and only row of user data
        @SuppressLint("Recycle")
        fun getFirstMealType() : String {
            val cursor = readableDatabase.rawQuery("SELECT * FROM MEALS", null)
            val mealType = cursor.getString(0)
            return mealType
        }
    }
}
