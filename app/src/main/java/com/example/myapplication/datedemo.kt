package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.sportnew.common.DefaultWheelDatePicker
import com.sportnew.common.WheelPickerDefaults
import com.sportnew.common.now
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus


@Composable
fun WheelDatePickerBottomSheet(
    modifier: Modifier,
    startDate: LocalDate = LocalDate.now(),
    onDateChange: (snappedDate: LocalDate) -> Unit = {},
) {
    DefaultWheelDatePicker(
        modifier = modifier,
        onSnappedDate = {
            onDateChange.invoke(it.snappedLocalDate)
            return@DefaultWheelDatePicker it.snappedIndex
        },
        startDate = startDate,
        showMonthAsNumber = true,
        showShortMonths = true,
        minDate = LocalDate.now().minus(29, DateTimeUnit.DAY),
        maxDate = LocalDate.now(),
        selectorProperties = WheelPickerDefaults.selectorProperties(
            borderColor = Color.LightGray
        ),
        rowCount = 3,
        height = 200.dp,
    )

}

@Composable
fun GreetingContent(
    startInitDate: LocalDate = LocalDate.now(),
    endInitDate: LocalDate = LocalDate.now()
) {
    val modify = remember {
        mutableStateOf(false)
    }
    val startDate = remember {
        mutableStateOf<LocalDate>(startInitDate)
    }
    val endDate = remember {
        mutableStateOf<LocalDate>(endInitDate)
    }
    val isSelectStart = remember {
        mutableStateOf(true)
    }
    LaunchedEffect(modify.value) {
        if (!modify.value) {
            modify.value = true
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "today",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.LightGray)
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        startDate.value = LocalDate.now()
                        endDate.value = LocalDate.now()
                        modify.value = false
                    }
            )
            Text(
                text = "yesterday",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.LightGray)
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        startDate.value = LocalDate
                            .now()
                            .minus(1, DateTimeUnit.DAY)
                        endDate.value = startDate.value
                        modify.value = false
                    }
            )
            Text(
                text = "lastweek",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.LightGray)
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        startDate.value = LocalDate
                            .now()
                            .minus(6, DateTimeUnit.DAY)
                        endDate.value = LocalDate.now()
                        modify.value = false
                    }
            )
            Text(
                text = "lastmonth",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.LightGray)
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        val yesterday = LocalDate
                            .now()
                            .minus(29, DateTimeUnit.DAY)
                        startDate.value =
                            LocalDate(yesterday.year, yesterday.month, yesterday.dayOfMonth)
                        endDate.value = LocalDate.now()
                        modify.value = false
                    }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = startDate.value.toString(),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = if (isSelectStart.value) Color.Blue else Color.Black,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fillMaxHeight()
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        isSelectStart.value = true
                    }
            )
            Text(
                text = endDate.value.toString(),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = if (isSelectStart.value) Color.Black else Color.Blue,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .wrapContentSize(align = Alignment.Center)
                    .clickable {
                        isSelectStart.value = false
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (modify.value) {
                WheelDatePickerBottomSheet(
                    modifier = Modifier
                        .fillMaxSize(),
                    onDateChange = {
                        Log.d("kkk", "onDateChange: $it")
                        if(isSelectStart.value){
                            startDate.value = it
                        }else{
                            endDate.value = it
                        }
                    },
                    startDate = if(isSelectStart.value )startDate.value else endDate.value
                )
            } else {
//                WheelDatePickerBottomSheet(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .fillMaxSize(),
//                    onDateChange = {
//
//                    },
//                    startDate = startDate.value
//                )
            }
        }


    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        GreetingContent()
    }
}