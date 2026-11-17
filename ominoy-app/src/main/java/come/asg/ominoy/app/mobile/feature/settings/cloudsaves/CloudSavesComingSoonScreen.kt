package come.asg.ominoy.app.mobile.feature.settings.cloudsaves

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import come.asg.ominoy.app.mobile.shared.compose.ui.OminoyHomeColors

@Composable
fun CloudSavesComingSoonScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(OminoyHomeColors.Background).padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = OminoyHomeColors.CardVariant,
            border = BorderStroke(1.dp, OminoyHomeColors.Line),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF7EE787).copy(alpha = .14f)) {
                    Box(Modifier.padding(horizontal = 21.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("☁", color = Color(0xFF7EE787), fontSize = 34.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "Cloud Saves",
                    color = OminoyHomeColors.Text,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Coming soon",
                    color = Color(0xFF7EE787),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Cloud save backup and sync are planned for a future update. Your existing local saves and save states remain on this device.",
                    color = OminoyHomeColors.Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
