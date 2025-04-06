package hu.cock.ripvessel.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.cock.ripvessel.ui.theme.RIPVesselTheme
import hu.gyulakiri.ripvessel.model.UserModel

@Composable
fun LoginScreen(onLoginSuccess: (UserModel) -> Unit, loginViewModel: LoginViewModel = viewModel()) {
    // State for username and password
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Collect the error message from the ViewModel
    val errorMessage by loginViewModel.errorMessage.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Text
            Text(
                text = "RIPVessel",
                modifier = Modifier.padding(bottom = 32.dp),
                style = MaterialTheme.typography.headlineMedium
            )
            // Username TextField with rounded corners
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Password TextField with rounded corners and password visual transformation
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Fancy Login Button with full-width that triggers login via the ViewModel
            Button(
                onClick = {
                    loginViewModel.login(username, password, onLoginSuccess)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Login")
            }
            // Display error message if credentials are invalid
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(color = Color.Red, shape = RoundedCornerShape(8.dp))
                        .padding(all = 8.dp)
                        .fillMaxWidth(),
                    content = {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    RIPVesselTheme {
        LoginScreen(onLoginSuccess = {})
    }
}