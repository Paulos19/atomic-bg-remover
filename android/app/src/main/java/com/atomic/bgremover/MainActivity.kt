    var serverUrl by remember { mutableStateOf("https://atomic-bg.khdya3.easypanel.host") }package com.atomic.bgremover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    secondary = Color(0xFF10B981),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AtomicBgRemoverScreen()
                }
            }
        }
    }
}

@Composable
fun AtomicBgRemoverScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var preserveWhiteDetails by remember { mutableStateOf(true) }
    var alphaMatting by remember { mutableStateOf(true) }
    var serverUrl by remember { mutableStateOf("https://atomic-bg.khdya3.easypanel.host") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            processedBitmap = null
            // Carrega original em bitmap
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho Premium
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Atomic Remover AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Preservação fina de roupas brancas e bordas",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card de Visualização da Imagem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Segmentando átomos e tecidos brancos...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else if (processedBitmap != null) {
                // Fundo xadrez / gradiente escuro para evidenciar a transparência perfeita
                Image(
                    bitmap = processedBitmap!!.asImageBitmap(),
                    contentDescription = "Resultado Sem Fundo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (originalBitmap != null) {
                Image(
                    bitmap = originalBitmap!!.asImageBitmap(),
                    contentDescription = "Imagem Original",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma imagem selecionada",
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Toque abaixo para escolher da galeria",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controles de Configuração Atômica
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Preservar Roupas Brancas", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Evita fusão com fundo em capuz/jaqueta", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = preserveWhiteDetails,
                        onCheckedChange = { preserveWhiteDetails = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1))
                    )
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Alpha Matting de Alta Precisão", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Refina fios de cabelo e fibras finas", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = alphaMatting,
                        onCheckedChange = { alphaMatting = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botões de Ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galeria")
            }

            Button(
                onClick = {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Selecione uma imagem primeiro!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val result = callRemoveBgApi(
                                context = context,
                                serverUrl = serverUrl,
                                imageUri = selectedImageUri!!,
                                preserveWhite = preserveWhiteDetails,
                                alphaMatting = alphaMatting
                            )
                            processedBitmap = result
                            Toast.makeText(context, "Fundo removido com precisão atômica!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = selectedImageUri != null && !isLoading,
                modifier = Modifier
                    .weight(1.3f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remover Fundo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

suspend fun callRemoveBgApi(
    context: Context,
    serverUrl: String,
    imageUri: Uri,
    preserveWhite: Boolean,
    alphaMatting: Boolean
): Bitmap = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val inputStream = context.contentResolver.openInputStream(imageUri)
        ?: throw Exception("Não foi possível ler o arquivo selecionado.")

    val byteArray = inputStream.readBytes()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            "upload.jpg",
            RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        )
        .build()

    val endpoint = "$serverUrl/api/remove-bg?alpha_matting=$alphaMatting&refine_foreground=$preserveWhite&model=birefnet-general"

    val request = Request.Builder()
        .url(endpoint)
        .post(requestBody)
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        throw Exception("Falha na API: Código ${response.code}")
    }

    val responseBytes = response.body?.bytes()
        ?: throw Exception("Resposta vazia da API")

    BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
        ?: throw Exception("Erro ao decodificar PNG resultante")
}
