package org.mariotaku.twidere.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.bumptech.glide.RequestManager
import com.google.android.material.card.MaterialCardView
import org.mariotaku.twidere.databinding.LayoutLinkPreviewBinding
import org.mariotaku.twidere.R


data class LinkPreviewData(
        val title: String?,
        val desc: String? = null,
        val img: String? = null,
        val imgRes: Int? = null
)


class LinkPreviewView : MaterialCardView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var binding: LayoutLinkPreviewBinding

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_link_preview, this, true)
        binding = LayoutLinkPreviewBinding.bind(view)
    }

    fun displayData(value: String, result: LinkPreviewData, requestManager: RequestManager) {
        binding.linkPreviewTitle.isVisible = true
        binding.linkPreviewLink.isVisible = true
        binding.linkPreviewImg.isVisible = result.img != null
        binding.linkPreviewLoader.isVisible = false
        binding.linkPreviewTitle.text = result.title
        binding.linkPreviewLink.text = Uri.parse(value).host
        if (result.img != null) {
            requestManager.load(result.img).into(binding.linkPreviewImg)
        } else if (result.imgRes != null) {
            requestManager.load(result.imgRes).into(binding.linkPreviewImg)
        }
    }

    fun reset() {
        binding.linkPreviewImg.isVisible = false
        binding.linkPreviewTitle.isVisible = false
        binding.linkPreviewLink.isVisible = false
        binding.linkPreviewLoader.isVisible = true
        binding.linkPreviewTitle.text = ""
    }
}