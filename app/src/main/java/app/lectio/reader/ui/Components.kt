package app.lectio.reader.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lectio.reader.data.Book
import app.lectio.reader.data.Epub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun BookCover(book: Book, modifier: Modifier = Modifier, compact: Boolean = false) {
    val bitmap by produceState<android.graphics.Bitmap?>(null,book.id) {
        value=withContext(Dispatchers.IO) { runCatching { book.cover?.let { val bytes=Epub.resource(File(book.file),it); val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds); var scale=1; while(bounds.outWidth/scale>600 || bounds.outHeight/scale>900) scale*=2; BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inSampleSize=scale }) } }.getOrNull() }
    }
    val colors=listOf(Color(0xFF293F5D),Color(0xFF3E425C),Color(0xFF2E4A53),Color(0xFF414956))
    val color=colors[(book.id.hashCode() and Int.MAX_VALUE)%colors.size]
    Box(modifier.clip(RoundedCornerShape(3.dp,8.dp,8.dp,3.dp)).background(color)) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),"Portada de ${book.title}",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else {
            Column(Modifier.fillMaxSize().padding(if(compact) 12.dp else 20.dp),verticalArrangement=Arrangement.SpaceBetween) {
                Text(if(book.demo) "LECTIO\nEDICIONES" else "BIBLIOTECA\nPERSONAL",color=Color(0xFFCCD7E6),fontSize=if(compact) 7.sp else 9.sp,lineHeight=12.sp,letterSpacing=1.sp)
                Text(book.title,color=Color(0xFFE6ECF5),fontFamily=Literary,fontSize=if(compact) 18.sp else 28.sp,lineHeight=if(compact) 23.sp else 34.sp,maxLines=5,overflow=TextOverflow.Ellipsis)
                Text(if(book.demo) "Una lectura para\nir más despacio" else book.author,color=Color(0xFFCCD7E6),fontSize=if(compact) 8.sp else 11.sp,maxLines=2,lineHeight=14.sp)
            }
            Box(Modifier.width(5.dp).fillMaxHeight().background(Color.Black.copy(alpha=.12f)))
        }
    }
}
@Composable fun EmptyState(icon: ImageVector, title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=40.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp)); Text(title,style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp));Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(action!=null) { Spacer(Modifier.height(24.dp));Button(onClick=onAction) { Text(action) } }
    }
}
@Composable fun SectionHeading(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
        trailing?.invoke()
    }
}
