package com.example.calorie_tracker

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

// Colors stored here since possibly adding themes to project
//val temporaryBackgroundColor = Color(0xFF1E1E78)  // Super blue
//val temporaryBackgroundColor = Color(0xFF0F0F3A) // dark blue
//val temporaryNutritionRowColor = Color(0xFF5A5A64)  // Gray color
//val temporaryNutritionRowColor = Color(0xFF3C3C45) // Darker gray color

// Below are what we decided on for default dark mode...
//val temporaryBackgroundColor = Color(0xFF1E1E32) // darkest blue (best IMO)
//val temporaryTextColor = Color.White
//val temporaryNutritionRowColor = Color(0xFF2D3C50)  // darker blue color


data class UserData(
    val name: String,
    val emailAddress: String, // could include???
    val age: Int,
    val weight: Double,
    // user inputs feet & inches separately and stored separately
    val inchHeight: Double,


//    val colorTheme: Int
    val gender: Int,

    var recommendedCalories: Int = 2000
)

data class foodMealInformation(
    val mealType: MealTypes,
    val Calories: Double,
    val Protein: Double,
    val Carbs: Double,
    val Fat: Double,
    val foodName: String
)

data class dateAndWieght(
    // does it being var or val matter for security purposes???  dont want to make set and get functions :(
    var mealType: MealTypes,  // add some date storing information?
    var weight: Double
)


class Server {

    // current user data used inside app
    var currentUserData: UserData? = null

    // set user data after it has been entered by the user
    fun setUserData(name: String, email: String, age: Int, weight: Double, inchHeight: Double, gender: Int) {
        currentUserData = UserData(
            name = name,
            emailAddress = email,
            age = age,
            weight = weight,
            inchHeight = inchHeight,

//            colorTheme = 1
            gender = gender,
            recommendedCalories = 0
        )

        //  Calorie intake values are based on the Mifflin-St Jeor equation
        if (gender == 1) {// female
            currentUserData!!.recommendedCalories = ((655 + (4.35 * weight) + (4.7 * inchHeight) - (4.67 * age)) * 1.55).roundToInt()
        } else { // male or other
            currentUserData!!.recommendedCalories = ((66 + (6.23 * weight) + (12.7 * inchHeight) - (6.75 * age)) * 1.55).roundToInt()
        }

    }



    // accessor for user data
    fun getUserData(): UserData? {
        return currentUserData
    }

    // function used for writing to sql database in ProfilePage
    fun userDataForSQL(): String {
//        return "'${currentUserData?.name}', '${currentUserData?.emailAddress}', ${currentUserData?.age}, ${currentUserData?.weight}, ${currentUserData?.inchHeight}"
        return "'${currentUserData?.name}', '${currentUserData?.emailAddress}', ${currentUserData?.age}, ${currentUserData?.weight}, ${currentUserData?.inchHeight}, ${currentUserData?.gender}, ${currentUserData?.recommendedCalories}"
    }

    fun userDataToString(): String {
        return "Name $currentUserData.name, Email: $currentUserData.email, Weight: $currentUserData.weight, Age: $currentUserData.age, Height: $currentUserData.height"
    }

}

enum class Screens {
    MAINSCREEN,
    FOODPAGE,
    GRAPHPAGE,
    WORKOUTPAGE,
    PROFILEPAGE
}

//
enum class MealTypes {
    BREAKFAST,
    LUNCH,
    DINNER,
    DESERT,
    SNACKS,
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}