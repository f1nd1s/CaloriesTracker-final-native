package com.example.calorie_tracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Class for the profilePageUI which is what the user sees in the ProfilePage
class ProfilePage {

    // globally accessible private variables so I don't have to pass 8 values into the update function
    private var name by mutableStateOf("")
    private var email by mutableStateOf("")
    private var weight by mutableStateOf("")
    private var age by mutableStateOf("")
    private var height by mutableStateOf("")
    private var targetWeight by mutableStateOf("")
    private var currentlySelectedGender: Gender = Gender.MALE // default male since defaulted radio button
    private var myUserData: UserData? = null
    private var currentColorTheme: ColorTheme = ColorTheme.DARK

    @Composable
    fun ProfilePageUI(navController: NavHostController, myColorThemeViewModel: ColorThemeViewModel, server: Server, dbman: DatabaseManager) {

        currentColorTheme = myColorThemeViewModel.currentColorTheme // sets for global variable so accessible for STOREDAPPINFORMATION DB

        // make variables rememberable and updated with Composable so that landscape doesn't clear text fields
        val nameRememberable = rememberSaveable { mutableStateOf(name) }
        val emailRememberable = rememberSaveable { mutableStateOf(email) }
        val weightRememberable = rememberSaveable { mutableStateOf(weight) }
        val ageRememberable = rememberSaveable { mutableStateOf(age) }
        val heightRememberable = rememberSaveable { mutableStateOf(height) }

        val isDataLoaded = remember { mutableStateOf(false) }

        // check if userProfile is already in database to fill in text fields if it is
        LaunchedEffect(true) { // LaunchedEffect so only pulls information once instead of always overwriting
            if (!isDataLoaded.value) { // if false
                dbman.checkForExistingUserData(
                    nameRememberable,
                    emailRememberable,
                    ageRememberable,
                    weightRememberable,
                    heightRememberable
                )
                isDataLoaded.value = true
            }
        }

        val targetWeightRememberable = rememberSaveable { mutableStateOf(targetWeight) }
        val currentlySelectedGenderRememberable = rememberSaveable { mutableStateOf(Gender.MALE) }

        name = nameRememberable.value
        email = emailRememberable.value
        weight = weightRememberable.value
        age = ageRememberable.value
        height = heightRememberable.value
        targetWeight = targetWeightRememberable.value
        currentlySelectedGender = currentlySelectedGenderRememberable.value

        // Column housing complete profilePage
        Column(modifier = Modifier.fillMaxSize().background(currentColorTheme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "",
                    Modifier.size(50.dp).clickable {
                        navController.popBackStack()
                    },
                    myColorThemeViewModel.currentColorTheme.textColor
                )
                Text(
                    "User Profile Data:",
                    fontSize = 20.sp,
                    color = currentColorTheme.textColor,
                    modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
                )

                Button(
                    onClick = { updateProfileInformation(dbman) },
                    modifier = Modifier.testTag("updateProfileButton")
                ) {
                    Text(text = "Update Profile",
                        fontSize = 15.sp,
                        color = currentColorTheme.textColor
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(4.dp).verticalScroll(rememberScrollState())) {

                // All the Profile Rows with their labels and their text fields
                ProfileRow("Name", nameRememberable) { nameRememberable.value = it}
                ProfileRow("Email", emailRememberable) { emailRememberable.value = it}
                ProfileRow("Weight (kg)", weightRememberable) { weightRememberable.value = it}
                ProfileRow("Age", ageRememberable) { ageRememberable.value = it}
                ProfileRow("Height (cm)", heightRememberable) { heightRememberable.value = it}


                addAllGenderRadioButtons()

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    color = currentColorTheme.rowColor,
                    thickness = 5.dp
                )

                // Row attached to bottom of screen to add targetWeight for STOREDAPPINFORMATION database
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
                ) {
                    ProfileRow("Target Weight", targetWeightRememberable, ) { updatedTargetWeight-> targetWeightRememberable.value = updatedTargetWeight}
                }

                // button to update target weight
                Button(
                    onClick = { updateTargetWeight(dbman) },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
                        .testTag("updateTargetWeightButton")
                ) {
                    Text(text = "Update Target Weight",
                        fontSize = 15.sp,
                        color = currentColorTheme.textColor
                    )
                }

            }

        }

    }

    // profileRow which houses the text fields and text that is used in the UI
    @Composable
    fun ProfileRow(label: String, mutableVar: MutableState<String>, onValueChange: (String) -> Unit) {

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(currentColorTheme.rowColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label:", fontSize = 20.sp, color = currentColorTheme.textColor, modifier = Modifier.padding(4.dp))

            // text field to enter information for corresponding profileRow
            TextField(
                value = mutableVar.value,
                onValueChange = { onValueChange(it) },
                label = { Text("Enter $label") },
                textStyle = TextStyle(fontSize = 20.sp),

                colors = OutlinedTextFieldDefaults.colors( // only non deprecated solution for background
                    focusedBorderColor = currentColorTheme.backgroundColor,
                    unfocusedBorderColor = currentColorTheme.backgroundColor,
                    focusedTextColor = currentColorTheme.textColor,
                    unfocusedTextColor = currentColorTheme.textColor
                ),
                modifier = Modifier.padding(8.dp).testTag("${label}-input") // test tagged
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Updates the users profile information in the database and the server
    fun updateProfileInformation(dbman: DatabaseManager) {
        val ageToInt = age.toIntOrNull()
        val weightToDouble = weight.toDoubleOrNull()
        val heightToDouble = height.toDoubleOrNull()
        val sqlString = server.userDataForSQL()

        if (ageToInt != null && weightToDouble != null && heightToDouble != null) {
            server.setUserData(name, email, ageToInt, weightToDouble, heightToDouble, currentlySelectedGender.ordinal)
            if (currentDateAndWieght != null) {
                currentDateAndWieght.weight = myUserData?.weight ?: 0.0 // 0.0 for error handling later...
            }
            CoroutineScope(Dispatchers.IO).launch { // threaded
                dbman.clearUserProfile()
                Log.d("@@@", "Sending SQL USERPROFILE Database following string: $sqlString")
                dbman.insertUserProfileData(sqlString)
            }
        } else { // error handling
            Log.d("Input Error", "Invalid input for either age, weight, or height: ")
            Log.d("Input Error", "'$age' - Must be an Integer")
            Log.d("Input Error", "'$weight' and '$height' - Must be Doubles")
        }
    }

    // Updtes the targetWeight in the database
    fun updateTargetWeight(dbman: DatabaseManager) {
        // check to make sure user inputted something, otherwise no nothing
        val targetWeightToInt = targetWeight.toIntOrNull()

        if (targetWeightToInt != null) { // if valid input from user
            CoroutineScope(Dispatchers.IO).launch {
                dbman.clearStoredAppData() // clears STOREDAPPINFORMATION database so only 1 row ever exists...
                dbman.insertUpdatedWeight(targetWeightToInt, currentColorTheme)
            }
        } else { // if input was invalid print error message to log
            Log.d("Input Error", "Invalid input for targetWeight: '$targetWeight' - Must be an Integer")
        }
    }

    // Adds the gender radio buttons Composable to the ProfilePage
    @Composable
    fun addAllGenderRadioButtons() {

        var selectedGenderRadioButton by remember { mutableStateOf("Male") }

        Column(modifier = Modifier.background(currentColorTheme.backgroundColor)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(
                        selected = (selectedGenderRadioButton == "Male"),
                        onClick = {
                            currentlySelectedGender = Gender.MALE; selectedGenderRadioButton = "Male"
                        }
                    )
                    Text(
                        text = "Male",
                        fontSize = 25.sp,
                        color = currentColorTheme.textColor,
                    )
                }


                Spacer(modifier = Modifier.height(12.dp))


                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(
                        selected = (selectedGenderRadioButton == "Female"),
                        onClick = {
                            currentlySelectedGender = Gender.FEMALE; selectedGenderRadioButton = "Female"
                        }
                    )
                    Text(
                        text = "Female",
                        fontSize = 25.sp,
                        color = currentColorTheme.textColor,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(
                        selected = (selectedGenderRadioButton == "Other"),
                        onClick = {
                            currentlySelectedGender = Gender.OTHER; selectedGenderRadioButton = "Other"
                        }
                    )
                    Text(
                        text = "Other",
                        fontSize = 25.sp,
                        color = currentColorTheme.textColor,
                    )
                }

            }
        }
    }
}
