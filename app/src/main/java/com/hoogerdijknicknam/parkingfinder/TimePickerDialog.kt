package com.hoogerdijknicknam.parkingfinder

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.format.DateFormat
import android.widget.TimePicker
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by snick on 4-1-2018.
 */
class TimePickerDialogFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {
    private val listeners: MutableList<TimePickerDialog.OnTimeSetListener> = ArrayList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()
        return TimePickerDialog(activity, this, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        listeners.forEach { it -> it.onTimeSet(view, hourOfDay, minute) }
    }

    fun addTimeSetListener(onTimeSetListener: TimePickerDialog.OnTimeSetListener) {
        listeners.add(onTimeSetListener)
    }

    fun removeTimeSetListener(onTimeSetListener: TimePickerDialog.OnTimeSetListener) {
        listeners.remove(onTimeSetListener)
    }
}