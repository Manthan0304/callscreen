package com.example.phonecallscreen 

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.phonecallscreen.ui.theme.PhonecallscreenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

data class CallLogEntry(
    val number: String,
    val name: String?,
    val duration: Long,
    val type: Int,
    val date: Long
)

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhonecallscreenTheme {
                PhoneApp()
            }
        }
    }
}
@Composable
fun PhoneApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Recent Calls") },
                    label = { Text("Recents") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Dialer") },
                    label = { Text("Dialer") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                    label = { Text("Contacts") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> RecentCallsScreen()
                1 -> DialerScreen()
                2 -> ContactsScreen()
            }
        }
    }
}

// Recent Calls Screen
@Composable
fun RecentCallsScreen() {
    val context = LocalContext.current
    var callLogs by remember { mutableStateOf(listOf<CallLogEntry>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!permissionStatus) {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
            error = "Call log permission is required to view recent calls"
            callLogs = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
            error = null
            callLogs = fetchCallLogs(context)
        } catch (e: Exception) {
            error = "Failed to load call logs: ${e.message}"
            callLogs = emptyList()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn {
                    items(callLogs) { call ->
                        CallLogItem(call)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun CircleAvatar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase().take(1),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun CallLogItem(call: CallLogEntry) {
    val context = LocalContext.current

    val displayName = if (!call.name.isNullOrBlank()) call.name else call.number
    val initial = displayName.take(1)

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${call.number}")
                }
                ContextCompat.startActivity(context, intent, null)
            } catch (e: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${call.number}")
                        }
                        ContextCompat.startActivity(context, intent, null)
                    } catch (e: Exception) {
                    }
                } else {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                text = initial,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = when (call.type) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            else -> "Unknown"
                        },
                        fontSize = 12.sp,
                        color = when (call.type) {
                            CallLog.Calls.INCOMING_TYPE -> Color.Green
                            CallLog.Calls.OUTGOING_TYPE -> Color.Blue
                            CallLog.Calls.MISSED_TYPE -> Color.Red
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(call.date)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// Dialer Screen
@Composable
fun DialerScreen() {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && phoneNumber.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                ContextCompat.startActivity(context, intent, null)
            } catch (e: Exception) {
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = phoneNumber,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            Divider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            DialerPad(
                onNumberClick = { digit ->
                    phoneNumber += digit
                },
                onDeleteClick = {
                    if (phoneNumber.isNotEmpty()) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                onCallClick = {
                    if (phoneNumber.isNotEmpty()) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            try {
                                val intent = Intent(Intent.ACTION_CALL).apply {
                                    data = Uri.parse("tel:$phoneNumber")
                                }
                                ContextCompat.startActivity(context, intent, null)
                            } catch (e: Exception) {
                            }
                        } else {
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }
                }
            )
        }
    }
}
@Composable
fun DialerPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val dialPadButtons = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "*", "0", "#"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(dialPadButtons) { digit ->
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNumberClick(digit) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onCallClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!permissionStatus) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            error = "Contacts permission is required to view your contacts"
            contacts = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
            error = null
            contacts = fetchContacts(context)
        } catch (e: Exception) {
            error = "Failed to load contacts: ${e.message}"
            contacts = emptyList()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn {
                    items(contacts) { contact ->
                        ContactItem(contact)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    val context = LocalContext.current
    val initial = contact.name.take(1)

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contact.phoneNumber?.let { number ->
                try {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$number")
                    }
                    ContextCompat.startActivity(context, intent, null)
                } catch (e: Exception) {
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    contact.phoneNumber?.let { number ->
                        try {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$number")
                            }
                            ContextCompat.startActivity(context, intent, null)
                        } catch (e: Exception) {
                        }
                    }
                } else {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                text = initial,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                contact.phoneNumber?.let { number ->
                    Text(
                        text = number,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

suspend fun fetchCallLogs(context: Context): List<CallLogEntry> = withContext(Dispatchers.IO) {
    try {
        val callLogs = mutableListOf<CallLogEntry>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            ),
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                val name = it.getString(nameIndex)
                val duration = it.getLong(durationIndex)
                val type = it.getInt(typeIndex)
                val date = it.getLong(dateIndex)

                callLogs.add(CallLogEntry(number, name, duration, type, date))
            }
        }
        callLogs
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun fetchContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
    try {
        val contacts = mutableListOf<Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)

                contacts.add(Contact(id, name, number))
            }
        }
        contacts
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun initiateCall(context: Context, phoneNumber: String) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            // Handle call initiation error
        }
    }
}