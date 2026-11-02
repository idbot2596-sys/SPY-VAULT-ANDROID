package com.spyvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF090B16)
private val Panel = Color(0xFF15182A)
private val Violet = Color(0xFF9B6CFF)
private val Muted = Color(0xFF9298B3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(9, 11, 22)
        setContent { SpyVaultScreen() }
    }
}

@Composable
private fun SpyVaultScreen() {
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(listOf("Google", "GitHub", "Instagram")) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Night, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17132B), Night)))) {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Violet.copy(alpha=.18f), RoundedCornerShape(15.dp)), Alignment.Center) {
                        Icon(Icons.Default.Lock, null, tint = Violet, modifier = Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SPY VAULT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
                        Text("YOUR PRIVATE DIGITAL SAFE", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                    Icon(Icons.Default.Fingerprint, null, tint = Violet, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(30.dp))
                Text("Good to see you.", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Everything important, secured in one place.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("YOUR VAULT", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(saved.size.toString().padStart(2,'0'), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(9.dp)); Text("SAVED ITEMS", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom=8.dp))
                        }
                        Text("Local demo data • Secure storage setup pending", color = Violet, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search your vault",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null,tint=Muted)},shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Panel,focusedBorderColor=Violet,unfocusedTextColor=Color.White,focusedTextColor=Color.White))
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("SAVED LOGINS",color=Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp,letterSpacing=1.3.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={showAdd=true}) { Icon(Icons.Default.Add,null); Text("ADD NEW") }
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                    items(saved.filter { it.contains(query,true) }.size) { i ->
                        val item = saved.filter { it.contains(query,true) }[i]
                        Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(17.dp),modifier=Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).background(Violet.copy(alpha=.16f),RoundedCornerShape(12.dp)),Alignment.Center) { Text(item.take(1),color=Violet,fontWeight=FontWeight.Bold,fontSize=18.sp) }
                                Spacer(Modifier.width(13.dp))
                                Column { Text(item,color=Color.White,fontWeight=FontWeight.SemiBold); Text("Login credential",color=Muted,fontSize=11.sp) }
                                Spacer(Modifier.weight(1f)); Text("••••••••",color=Muted,fontSize=12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("SPY VAULT • PRIVATE BY DESIGN",color=Muted,fontSize=9.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(vertical=16.dp))
            }
            if(showAdd) AlertDialog(onDismissRequest={showAdd=false},title={Text("Add vault item")},text={
                Column { OutlinedTextField(value=title,onValueChange={title=it},label={Text("Website / title")}); OutlinedTextField(value=username,onValueChange={username=it},label={Text("Username")}) }
            },confirmButton={TextButton(onClick={if(title.isNotBlank()) saved=saved+title; title="";username="";showAdd=false}){Text("Save")}},dismissButton={TextButton(onClick={showAdd=false}){Text("Cancel")}})
        }
    }
}
