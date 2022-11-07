package com.blackdiamondstudios.android.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

//we need a create a drawable view class type which considers inputs on a GUI
class DrawingView(context: Context , attrs: AttributeSet) : View(context, attrs)
{

    //setting up the graphics variables and objects to the classes
    //use ctrl click over the class type to know more about it
    private var mDrawPath : CustomPath?= null // a variable of custom path inner class to use it further
    private var mCanvasBitmap: Bitmap?= null // an instance of the Bitmap class
    private var mDrawPaint: Paint?= null// the paint class which holds the style and color information about how to draw
    private var mCanvasPaint: Paint ?= null // instance of the canvas paint view
    private var mBrushSize : Float = 0.toFloat()// a variable for stroke/brush size to draw on the canvas
    private var color = Color.BLACK  //a variable to hold a color instance of the stroke
    private var canvas: Canvas ?= null

    private val mPaths = ArrayList<CustomPath>()
private var mUndoPaths = ArrayList<CustomPath>()
    private var mRedoPaths = ArrayList<CustomPath>()


    init{
        setUpDrawing()

    }
    fun onCLickUndo(){   //class to implement the undo functionality
        if(mPaths.size >0 ){
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1)) //to store the last entry of the mPaths and remove it at the last array entry storage value
                invalidate()

        }
    }

    fun onClickRedo(){
        if(mPaths.size <= 0 ){
            mRedoPaths.remove(mPaths.removeAt(mPaths.size + 1) )
                invalidate()
        }
    }

    private fun setUpDrawing (){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG) //adding the graphics to the memory
        //mBrushSize = 20.toFloat()  // here the default or we can initial brush/stroke size float value

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
         super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)

    }
// change canvas to canvas? if it fails
    override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawBitmap(
        mCanvasBitmap!!,
        0f,
        0f,
        mCanvasPaint
    ) //defining the borders of the drawable canvas view in a matrix form
    //read the documentations of the courses for more understanding

    for(path in mPaths){
        mDrawPaint!!.strokeWidth = path.brushThickness
        mDrawPaint!!.color = path.color
        canvas.drawPath(path, mDrawPaint!!)
    }


    if (!mDrawPath!!.isEmpty){
        mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
        mDrawPaint!!.color = mDrawPath!!.color
        canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
         val  touchX = event.x
        val touchY = event.y

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()

                mDrawPath!!.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE ->{
                mDrawPath!!.lineTo(touchX , touchY)
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false  //for all the other motion event actions not considered
        }
        invalidate()

        return true
    }


    fun setSizeForBrush(newSize : Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
            )
            mDrawPaint!!.strokeWidth = mBrushSize

    }

    fun setColor(newColor: String){
        color= Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    internal inner class CustomPath(var color: Int , var brushThickness : Float ) : Path() {

    }
}