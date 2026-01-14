package org.mariotaku.twidere.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import org.mariotaku.twidere.databinding.ActivityDeviceIncompatibleBinding
import org.mariotaku.twidere.BuildConfig
import org.mariotaku.twidere.R
import java.util.*

/**
 * Created by mariotaku on 16/4/4.
 */
class IncompatibleAlertActivity : Activity() {

    private lateinit var binding: ActivityDeviceIncompatibleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceIncompatibleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.infoText.append(String.format(Locale.US, "Twidere version %s (%d)\n",
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        binding.infoText.append(String.format(Locale.US, "Classpath %s\n", ClassLoader.getSystemClassLoader()))
        binding.infoText.append(String.format(Locale.US, "Brand %s\n", Build.BRAND))
        binding.infoText.append(String.format(Locale.US, "Device %s\n", Build.DEVICE))
        binding.infoText.append(String.format(Locale.US, "Display %s\n", Build.DISPLAY))
        binding.infoText.append(String.format(Locale.US, "Hardware %s\n", Build.HARDWARE))
        binding.infoText.append(String.format(Locale.US, "Manufacturer %s\n", Build.MANUFACTURER))
        binding.infoText.append(String.format(Locale.US, "Model %s\n", Build.MODEL))
        binding.infoText.append(String.format(Locale.US, "Product %s\n", Build.PRODUCT))
    }

}
