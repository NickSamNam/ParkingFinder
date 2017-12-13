package com.hoogerdijknicknam.parkingfinder

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.format.DateFormat
import android.widget.TimePicker
import java.util.*

/**
 * Created by snick on 13-12-2017.
 */
class TimePickerFragment() : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    private var onTimeSetListener: TimePickerDialog.OnTimeSetListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return TimePickerDialog(activity, this, h, m, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        onTimeSetListener?.onTimeSet(view, hourOfDay, minute)
    }

    fun setOnTimeSetListener(onTimeSetListener: TimePickerDialog.OnTimeSetListener) {
        this.onTimeSetListener = onTimeSetListener
    }
}