package com.eyrafabdullayev.landmarkbookv1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.decodeBitmap
import kotlinx.android.synthetic.main.activity_add.*
import java.io.ByteArrayOutputStream

class AddActivity : AppCompatActivity() {

    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        val intent = intent
        val info = intent.getStringExtra("info")


        if(info.equals("details",true)){
            addButton.visibility = View.INVISIBLE
            val id = intent.getIntExtra("id",1)

            try {

                val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

                val sqlString = "SELECT * FROM arts WHERE id = ?"
                val cursor = database.rawQuery(sqlString, arrayOf(id.toString()))

                val artNameIx = cursor.getColumnIndex("artname")
                val artistNameIx = cursor.getColumnIndex("artistname")
                val yearIx = cursor.getColumnIndex("year")
                val imageIx = cursor.getColumnIndex("image")

                while (cursor.moveToNext()){
                    artNameText.setText(cursor.getString(artNameIx))
                    artistNameText.setText(cursor.getString(artistNameIx))
                    yearText.setText(cursor.getString(yearIx))

                    val byteArray = cursor.getBlob(imageIx)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }

        }else if(info.equals("add",true)){
            artNameText.setText("")
            artistNameText.setText("")
            yearText.setText("")
            addButton.visibility = View.VISIBLE

            val bitmap = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.choose)
            imageView.setImageBitmap(bitmap)
        }
    }

    fun selectImage(view: View){

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }else{
            val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent,2)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode == 1){

            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent,2)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == 2){

            if(resultCode == Activity.RESULT_OK && data != null){
                selectedPicture = data.data

                try {
                    if(selectedPicture != null){
                       if(Build.VERSION.SDK_INT > 28){
                           val source = ImageDecoder.createSource(this.contentResolver,
                               selectedPicture!!
                           )
                           selectedBitmap = ImageDecoder.decodeBitmap(source)
                           imageView.setImageBitmap(selectedBitmap)
                       }else{
                           selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPicture)
                           imageView.setImageBitmap(selectedBitmap)
                       }
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun save(view: View){

        val artNameText = artNameText.text.toString()
        val artistNameText = artistNameText.text.toString()
        val yearText = yearText.text.toString()

        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artNameText)
                statement.bindString(2,artistNameText)
                statement.bindString(3,yearText)
                statement.bindBlob(4,byteArray)

                statement.execute()
            } catch (e: Exception){
                e.printStackTrace()
            }

            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) //it clear all the activities which is open
            startActivity(intent)
        }else{
            Toast.makeText(this,"You must select some picture!", Toast.LENGTH_LONG).show()
        }
    }

    fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if(bitmapRatio > 1){
            width = maximumSize
            val scaledHeight = width / bitmapRatio;
            height = scaledHeight.toInt()
        }else{
            height = maximumSize
            val scaledHeight = height * bitmapRatio
            width =  scaledHeight.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun back(view: View){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) //it clear all the activities which is open
        startActivity(intent)
    }
}
