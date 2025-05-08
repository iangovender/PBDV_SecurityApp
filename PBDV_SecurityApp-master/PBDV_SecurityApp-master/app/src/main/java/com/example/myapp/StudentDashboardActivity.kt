
package com.example.myapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.jvm.java


class StudentDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MaterialTheme {

                Surface(modifier = Modifier.fillMaxSize()) {

                    SimplifiedStudentDashboardScreen(
                        onGetLocation = {
                            // Define the action for the button click: Start MainActivity
                            // Now the compiler knows what MainActivity is because of the import
                            startActivity(Intent(this, MainActivity::class.java))
                            // Optional: finish() // You might want to finish this activity
                            // if MainActivity is the primary location screen.

                        },
                        onReportIncident = {
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("destination", "reportIncident")
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

/**
 * A simplified Composable function for the Student Dashboard.
 * It only displays a title and a button to navigate to the location screen.
 *
 * @param onGetLocation Lambda function to execute when the "Get Location" button is clicked.
 */
@Composable
fun SimplifiedStudentDashboardScreen(
    onGetLocation: () -> Unit,
    onReportIncident: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Student Dashboard",
            style = MaterialTheme.typography.headlineMedium // Use a prominent text style
        )


        Spacer(modifier = Modifier.height(32.dp))


        Button(
            onClick = onGetLocation,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Get Location")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onReportIncident,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Report Incident")
        }
    }
}