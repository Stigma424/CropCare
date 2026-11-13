package com.example.cropcare

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth

/** One account-scoped photo source for every personal avatar on this device. */
object UserPhoto {
    fun bind(view: ImageView, fallback: View? = null) {
        view.setImageDrawable(null)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val saved = uid?.let { view.context.getSharedPreferences("CropCarePhotos", Context.MODE_PRIVATE).getString(it, null) }
        val bitmap = runCatching {
            if (saved == null) null else {
                val uri = Uri.parse(saved)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                view.context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 512 || bounds.outHeight / inSampleSize > 512) inSampleSize *= 2
                }
                view.context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
        }.getOrNull()
        view.imageTintList = null
        view.setPadding(0, 0, 0, 0)
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        view.background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xffe9f5e9.toInt()) }
        view.clipToOutline = true
        if (bitmap != null) { view.setImageBitmap(bitmap); view.visibility = View.VISIBLE; fallback?.visibility = View.INVISIBLE }
        else if (fallback != null) { view.visibility = View.GONE; fallback.visibility = View.VISIBLE }
        else { view.visibility = View.VISIBLE; view.setImageResource(R.drawable.cc_avatar) }
    }
}
