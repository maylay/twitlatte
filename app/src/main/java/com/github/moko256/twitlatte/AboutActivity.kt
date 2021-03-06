/*
 * Copyright 2015-2019 The twitlatte authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.moko256.twitlatte

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by moko256 on 2018/04/21.
 *
 * @author moko256
 */
private const val birthDayMillis = 1446956982000L

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_back_white_24dp)
        }

        findViewById<ImageView>(R.id.icon).setImageDrawable(
                packageManager.getApplicationIcon(BuildConfig.APPLICATION_ID)
        )
    }

    fun onPictureClick(@Suppress("UNUSED_PARAMETER") view: View) {
        Toast.makeText(
                this,
                getString(
                        R.string.birthday_of_this_app_is,
                        birthDayMillis
                ) + "\n\n" + getString(
                        R.string.age_of_this_app_is,
                        (System.currentTimeMillis() - birthDayMillis) / 31557600000L
                ),
                Toast.LENGTH_LONG
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}