package com.blackdiamondstudios.android.kidsdrawingapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView ?= null
    private var mImageButtonCurrentPaint: ImageButton ?=null
    var customProgressDialog : Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                    if(result.resultCode == RESULT_OK && result.data !=null ){
                        val imageBackground: ImageView = findViewById(R.id.iv_background)

                        imageBackground.setImageURI(result.data?.data)   //URI is the location path of the image from the device storage after the permission is granted
                    }
        }

    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted) {
                    Toast.makeText(
                        this@MainActivity ,
                        "Permission granted now so you can read the storage",
                        Toast.LENGTH_SHORT
                    ).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                openGalleryLauncher.launch(pickIntent)


                } else {
                    if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity ,
                            "Oops, the permission was denied!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)

        )

        val ib_brush: ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{

            requestStoragePermission()

        }
        val ib_undo: ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener{
                drawingView?.onCLickUndo()  //to refer tot he undo functionality from the undo class
        }

        /* val ib_redo: ImageButton = findViewById(R.id.ib_redo)
        ib_redo.setOnClickListener{
            drawingView?.onClickRedo()  //to refer to the redo functionality from the redo class
        } */
        val ib_save: ImageButton = findViewById(R.id.ib_save)
        ib_save.setOnClickListener{
            if(isReadStorageAllowed()){
    lifecycleScope.launch{
        showProgressDialog()
        val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
        val myBitmap: Bitmap = getBitmapFromVIew(flDrawingView)
        saveBitmapFile(myBitmap)

    }
}

            //to refer tot he undo functionality from the undo class
        }
    }
    @SuppressLint("ResourceType")
    private fun showBrushSizeChooserDialog(){
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val meduimBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        meduimBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View){
        Toast.makeText(this, "Clicked paint", Toast.LENGTH_LONG).show()
        if(view !== mImageButtonCurrentPaint){ //to select the palette to highlight from the rest at a time using palette pressed and palette normal
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()  //enables the readability of the string on the tag
            drawingView?.setColor(colorTag)


            imageButton!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }

    }

    private fun isReadStorageAllowed(): Boolean {
        var result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
        {
            showRationaleDialog("kids drawing app", "Kids drawing app" +
            "needs to access your external storage")

        } else {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            ))
        }

    }

    //to display the alert dialog for the permission
    private fun showRationaleDialog(
        title: String,
        message :String,
    ) {
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }




// we need to convert the image as a view to a bitmap to be able to store it in the memory after having granted the required permissions
    private fun getBitmapFromVIew(view :View ): Bitmap {

    //define a bitmap with the same size as the view
    //create Bitmap: returns a mutale bitmap with the specified width adn height
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height, Bitmap.Config.ARGB_8888)
    //bind a canvas to it
        val canvas = Canvas(returnedBitmap)
    //get the view's background
            val bgDrawable = view.background
    if(bgDrawable!= null){
        //has background drawable, then draw it on the canvas
        bgDrawable.draw(canvas)
    }else {
        //does not have backgorund drawable then draw white backgorund on the canvas
        canvas.drawColor(Color.WHITE)
    }
    //draw the view on the canvas
    view.draw(canvas)
//return the bitmap
    return returnedBitmap
        }

private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
    var result = ""
    withContext(Dispatchers.IO){
        if(mBitmap != null) {
            try {
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                val f = File(externalCacheDir?.absoluteFile.toString()
                        + File.separator +  "kidsDrawingApp_" + System.currentTimeMillis() /1000 + ".png"

                )

                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()

                result = f.absolutePath

                runOnUiThread{
                    cancelProgressDialog()
                    if(result.isNotEmpty()){
                        Toast.makeText(this@MainActivity,
                        "file saved succesfully : $result",
                        Toast.LENGTH_LONG
                        ).show()

                        shareImage(result)

                    }else {
                        Toast.makeText(this@MainActivity,
                            "Something went worn while trying to save the file ",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }

            }
            catch(e:Exception){
                result = ""
                e.printStackTrace()
            }
        }
    }
    return result
}

    //the progress dialog implemented to show a wwaiting UI for the image saving process
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }
    //class to cancel the ongoing waiting UI when the image saved is done
        private fun cancelProgressDialog(){
    if(customProgressDialog != null){
        customProgressDialog?.dismiss()
        customProgressDialog = null
    }
        }
//class to implement the sharing of the image in the on_create method
    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
           path, uri ->
           val sharedIntent = Intent()
           sharedIntent.action = Intent.ACTION_SEND
            sharedIntent.putExtra(Intent.EXTRA_STREAM, uri)
            sharedIntent.type = "image/png"
            startActivity(Intent.createChooser(sharedIntent, "share" ) )
        }
    }
}
