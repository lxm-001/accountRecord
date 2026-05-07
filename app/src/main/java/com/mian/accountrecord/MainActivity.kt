package com.mian.accountrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mian.accountrecord.domain.usecase.CheckAuthStateUseCase
import com.mian.accountrecord.ui.navigation.MainScaffold
import com.mian.accountrecord.ui.theme.AccountRecordTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkAuthStateUseCase: CheckAuthStateUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccountRecordTheme {
                MainScaffold(checkAuthStateUseCase = checkAuthStateUseCase)
            }
        }
    }
}
