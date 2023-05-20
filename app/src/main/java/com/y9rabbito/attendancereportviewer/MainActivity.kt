package com.y9rabbito.attendancereportviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.google.android.material.textfield.TextInputLayout
import lecho.lib.hellocharts.model.PieChartData
import lecho.lib.hellocharts.model.SliceValue
import lecho.lib.hellocharts.view.PieChartView
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.YearMonth


class MainActivity : AppCompatActivity() {
    @SuppressLint("ResourceType")
    var document: PdfDocument? = null

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)


        var spinner: Spinner = findViewById(R.id.month_spinner)
        var clearButton = findViewById<AppCompatButton>(R.id.clear_number_button)
        var editNumber: EditText = findViewById(R.id.search_roll_number)
        var generateReportButton = findViewById<AppCompatButton>(R.id.generate_report_button)
        var editNumberBox = findViewById<TextInputLayout>(R.id.search_roll_number_outside)
        var yearButton = findViewById<Button>(R.id.year_button)
        var yearShow = findViewById<TextView>(R.id.year_view)
//        var yearInput = findViewById<EditText>(R.id.year_input)
        var pdfGenerateButton: Button = findViewById(R.id.generatePDFButton)
        var nameView = findViewById<TextView>(R.id.name_view)

//        var pieChartView = findViewById<PieChartView>(R.id.chart)

        //Text-Change Listening Mode
        editNumber.addTextChangedListener {
            val xrp: XmlResourceParser = resources.getXml(R.drawable.textview_selector)
            val csl: ColorStateList = ColorStateList.createFromXml(resources, xrp)
            editNumber.setTextColor(csl)
            editNumber.setBackgroundResource(R.drawable.custom_search_layout)
//            pieChartView.visibility = View.INVISIBLE
            pdfGenerateButton.visibility = View.INVISIBLE
            nameView.visibility = View.INVISIBLE
            yearShow.text = "Year"
        }


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this, R.array.month_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }


        //Clear Button Handling
        clearButton.setOnClickListener {
            editNumber.text.clear()
//            yearInput.text.clear()
            nameView.visibility = View.INVISIBLE
            pdfGenerateButton.visibility = View.INVISIBLE
            yearShow.text = "Year"
        }

        generateReportButton.setOnClickListener {
            var number: String = editNumber.text.trim().toString()
            var year: String = yearShow.text.toString().trim().takeLast(2)
            var type: Int = 12

            type = getMonthSelection()

            var month: String = type.toString()

            if (month.length == 1) {
                month = "0$month"
            }


            if (isNetworkConnected()) {
                if (number.isEmpty() || yearShow.text == "Year") {
                    editNumber.setBackgroundResource(R.drawable.custom_search_layout)
                    Toast.makeText(this, "Fill the fields", Toast.LENGTH_SHORT).show()
                } else {
                    getAttendance(number, month, year)
                }
            } else {
                Toast.makeText(this, "No Internet!", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun getMonthSelection(): Int {
        var spinner: Spinner = findViewById(R.id.month_spinner)
        var monthSelection = spinner.selectedItem.toString()
        var type: Int = 1
        type = when (monthSelection) {
            "January" -> 1
            "February" -> 2
            "March" -> 3
            "April" -> 4
            "May" -> 5
            "June" -> 6
            "July" -> 7
            "August" -> 8
            "September" -> 9
            "October" -> 10
            "November" -> 11
            else -> 12
        }
        return type
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    private fun getAttendance(roll: String, month: String, year: String) {
        var spinner: Spinner = findViewById(R.id.month_spinner)
        var clearButton = findViewById<AppCompatButton>(R.id.clear_number_button)
        var editNumber: EditText = findViewById(R.id.search_roll_number)
        var pdfGenerateButton = findViewById<Button>(R.id.generatePDFButton)
        var editNumberBox = findViewById<TextInputLayout>(R.id.search_roll_number_outside)
        var yearButton = findViewById<Button>(R.id.year_button)
        var yearInput = findViewById<TextView>(R.id.year_view)
        var nameView = findViewById<TextView>(R.id.name_view)

        var url =
            "https://attendanceapi-production.up.railway.app/attendance/$roll/$month/$year"

//        println(url)

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, {
            var monthSelection = spinner.selectedItem.toString()
            var year = yearInput.text.toString()
            var attendanceCount = it.length()


            var yearMonthObject = YearMonth.of(year.toInt(), month.toInt())
            var daysInMonth = yearMonthObject.lengthOfMonth()


            try {
                var jsonOBJECT1 = it.getJSONObject(0)
                var name = jsonOBJECT1.getString("name")

                var list = mutableListOf<String>()


//                var pieChartView: PieChartView = findViewById(R.id.chart)
//                var pieData = mutableListOf<SliceValue>()

                for (i in 0 until attendanceCount) {
                    var jsonobject = it.getJSONObject(i)
                    var date = jsonobject.getString("date").toString()
                    list.add(date)
                }
                nameView.text =
                    "Student Name: $name \r\nMonth: $monthSelection,$year \r\nStudent ID: $roll\r\nTotal Attendance: $attendanceCount/$daysInMonth"
                nameView.visibility = View.VISIBLE
                pdfGenerateButton.visibility = View.VISIBLE


                pdfGenerateButton.setOnClickListener {
                    generatePDF(
                        name,
                        month,
                        year,
                        roll,
                        list,
                        attendanceCount.toFloat(),
                        daysInMonth.toFloat()
                    )
                }
                //Pie-Chart Code
//                var presentCount: Float = attendanceCount/30*100.toFloat();
//                var absentCount: Float = (30-attendanceCount)/30*100.toFloat();
//                var absentCount: Float = 40.0.toFloat()
//                var presentCount: Float = 60.0.toFloat()
//
//                pieData.add(SliceValue(presentCount, Color.CYAN))
//                pieData.add(SliceValue(absentCount, Color.RED))
//                //getting the total attendance
//                var pieChartData = PieChartData(pieData)
//                pieChartView.pieChartData = pieChartData
//                pieChartView.visibility = View.VISIBLE


            } catch (e: Exception) {
                nameView.text = "No records found! for $monthSelection,$year"
            }

        }, {
            nameView.text = null
            Toast.makeText(this, "Something Went Wrong!", Toast.LENGTH_SHORT).show()
        })



        editNumber.hideKeyboard()
        MySingleton.getInstance(this).addToRequestQueue(jsonArrayRequest)
    }

    private fun generatePDF(
        name: String,
        month: String,
        year: String,
        roll: String,
        list: MutableList<String>,
        attendanceCount: Float,
        daysInMonth: Float
    ) {

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.pdf_layout, null)
        createPdfFromView(view, name, month, year, roll, list, attendanceCount, daysInMonth)
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private val CREATE_FILE = 1

    //Pdf Generate Activity


    fun createPdfFromView(
        view: View,
        name: String,
        month: String,
        year: String,
        roll: String,
        list: MutableList<String>,
        attendanceCount: Float,
        daysInMonth: Float
    ) {
        var studentName: TextView = view.findViewById<TextView>(R.id.studentName)
        var studentRoll: TextView = view.findViewById<TextView>(R.id.studentRollNumber)
        var studentAttendance: TextView = view.findViewById<TextView>(R.id.studentTotalAttendance)
        var studentPercentage: TextView = view.findViewById(R.id.studentPercentageAttendance)
        var dates: TextView = view.findViewById(R.id.studentAttendanceDates)
        var pieChartView: PieChartView = view.findViewById(R.id.chart)

        studentName.text = name
        studentRoll.text = "Student Roll No: $roll"
        studentAttendance.text = "Attendance: ${attendanceCount.toInt()}/${daysInMonth.toInt()}"
        var percentage: Float = (attendanceCount.toFloat() / daysInMonth.toFloat()) * 100.toFloat()
        studentPercentage.text = "Attendance: $percentage%"

        var dateSet: String = "Dates: "
        for (item in list) {
            dateSet += "${item}, "
        }

        dates.text = "$dateSet"

        //Pie-Chart
        var pieData = mutableListOf<SliceValue>()
        var presentCountFloat: Float = percentage
        var absentCountFloat: Float = (daysInMonth - attendanceCount) / daysInMonth * 100.toFloat();

        pieData.add(
            SliceValue(
                presentCountFloat, Color.GREEN
            ).setLabel(("Present $presentCountFloat").toCharArray())
        )
        pieData.add(
            SliceValue(
                absentCountFloat,
                Color.BLUE
            ).setLabel(("Absent $absentCountFloat").toCharArray())
        )

        var pieChartData = PieChartData(pieData)
        pieChartView.pieChartData = pieChartData
        var dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.setCancelable(true)

        var lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = lp
        dialog.show()
        var downloadButton: Button = dialog.findViewById(R.id.downloadButton)
        downloadButton.setOnClickListener {
            generatePDFfromView(dialog.findViewById(R.id.innerBox))
        }
    }

    private fun generatePDFfromView(it: View) {
        var bitmap: Bitmap = getBitmapFromView(it)
        document = PdfDocument()
        val pageInfo = PageInfo.Builder(
            bitmap.width, bitmap.height, 1
        ).create()
        val myPage = document!!.startPage(pageInfo)
        var canvas: Canvas = myPage.canvas
        canvas.drawBitmap(bitmap, 0.toFloat(), 0.toFloat(), null)
        document!!.finishPage(myPage)
        createFile()
    }

    private fun createFile() {
        var intent: Intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_TITLE, "attendance.pdf")
        startActivityForResult(intent, CREATE_FILE);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            var uri: Uri? = null
            if (data != null) {
                uri = data.data
                if (document != null) {
                    var pfd: ParcelFileDescriptor? = null
                    try {
                        pfd = contentResolver.openFileDescriptor(uri!!, "w")
                        val fileOutPutStream = FileOutputStream(pfd!!.fileDescriptor)
                        document!!.writeTo(fileOutPutStream)
                        document!!.close()
                        Toast.makeText(this, "Pdf saved successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        try {
                            DocumentsContract.deleteDocument(contentResolver, uri!!)
                        } catch (ex: FileNotFoundException) {
                            ex.printStackTrace()
                        }
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "URI null", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getBitmapFromView(it: View): Bitmap {
        var returnedBitmap: Bitmap =
            Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
        var canvas: Canvas = Canvas(returnedBitmap)
        var bgDrawable: Drawable = it.background
        bgDrawable.draw(canvas)
        it.draw(canvas)
        return returnedBitmap
    }

    fun selectYear(view: View) {
        createDialogWithoutDateField(view)
    }

    private fun createDialogWithoutDateField(view: View) {

        var yearShow = findViewById<TextView>(R.id.year_view)

        val alertDialog: AlertDialog?
        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        val inflater = this.layoutInflater

        val cal = Calendar.getInstance()

        val dialog = inflater.inflate(R.layout.month_year_picker_dialog, null)
        val monthPicker = dialog.findViewById(R.id.picker_month) as NumberPicker
        val yearPicker = dialog.findViewById(R.id.picker_year) as NumberPicker

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = cal.get(Calendar.MONTH) + 1

        val year = cal.get(Calendar.YEAR)
        yearPicker.minValue = 1900
        yearPicker.maxValue = 3500
        yearPicker.value = year

        builder.setView(dialog)
            .setPositiveButton(Html.fromHtml("<font color='#537188'>Ok</font>")) { dialogInterface, which ->
                //Toast.makeText(applicationContext,"clicked yes",Toast.LENGTH_LONG).show()

                val value = yearPicker.value
                dialogInterface.cancel()
                yearShow.text = value.toString()

            }

        builder.setNegativeButton(Html.fromHtml("<font color='#CBB279'>Cancel</font>")) { dialogInterface, which ->
            dialogInterface.cancel()
            yearShow.text = "Year"
        }

        alertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

}