package com.example.cropcare

import android.app.Activity
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageView

object PasswordVisibility {
    fun bind(activity: Activity, fieldId: Int, toggleId: Int) {
        val field = activity.findViewById<EditText>(fieldId)
        val toggle = activity.findViewById<ImageView>(toggleId)
        toggle.setOnClickListener {
            val show = field.transformationMethod is PasswordTransformationMethod
            field.transformationMethod = if (show) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
            toggle.contentDescription = if (show) "Hide password" else "Show password"
            toggle.isSelected = show
        }
    }
}
