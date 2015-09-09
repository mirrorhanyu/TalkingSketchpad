package com.android.mirror.drawpicture;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.graphics.PorterDuff;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;

public class DrawingView extends View{

    //infos
    private String drawInfo;
    private String messageInfo;

    //m refers to myself, o refers to other friends
    private Path mPath, oPath;
    private Paint mPaint, oPaint;

    //draw items
    private int paintColor = 0xFF660000;
    private float brushSize;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    //handler to handler message in the main thread
    private Handler handler;

    //username
    private String username;

    //for callback
    private DanmakuMessageListener danmakuMessageListener;

    private Socket socket;
    {
        try {
            socket = IO.socket("http://your_server_ip:3000/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SetupDrawing();
        handler = new MyDrawHandler();
    }

    private void SetupDrawing(){
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(paintColor);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        oPath = new Path();
        oPaint = new Paint();
        oPaint.setColor(paintColor);
        oPaint.setAntiAlias(true);
        oPaint.setStrokeWidth(10);
        oPaint.setStyle(Paint.Style.STROKE);
        oPaint.setStrokeJoin(Paint.Join.ROUND);
        oPaint.setStrokeCap(Paint.Cap.ROUND);

        brushSize = getResources().getInteger(R.integer.small_size);
        mPaint.setStrokeWidth(brushSize);
        oPaint.setStrokeWidth(brushSize);

        socket.on("new message", onNewMessage);
        socket.connect();
        socket.emit("add user", username);
    }

    interface DanmakuMessageListener{
        void onMessageCome(String msg);
    }

    public void setDanmakuMessageListener(DanmakuMessageListener l){
        this.danmakuMessageListener = l;
    }

    class MyDrawHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            String message = (String)msg.obj;
            try {
                JSONObject jm = new JSONObject(message);
                String action = jm.getString("action");
                if(action.equals("ACTION_DOWN")){
                    oPath.moveTo(Float.parseFloat(jm.getString("x")), Float.parseFloat(jm.getString("y")));
                    invalidate();
                }else if(action.equals("ACTION_MOVE")){
                    oPath.lineTo(Float.parseFloat(jm.getString("x")), Float.parseFloat(jm.getString("y")));
                    invalidate();
                }else if(action.equals("DANMAKU")){
                    danmakuMessageListener.onMessageCome(jm.getString("danmaku"));
                }else if(action.equals("CHANGE_COLOR")){
                    paintColor = Color.parseColor(jm.getString("color"));
                    oPaint.setColor(paintColor);
                }else{
                    drawCanvas.drawPath(oPath, oPaint);
                    oPath.reset();
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, mPaint);
        canvas.drawPath(mPath, mPaint);
        canvas.drawBitmap(canvasBitmap, 0, 0, oPaint);
        canvas.drawPath(oPath, oPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(touchX, touchY);
                drawInfo = "{'action':'ACTION_DOWN' , 'x':"+ touchX + ",'y':" +  touchY+ "}";
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(touchX, touchY);
                drawInfo = "{'action':'ACTION_MOVE' , 'x':"+ touchX + ",'y':" +  touchY+ "}";
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                drawInfo = "{'action':'ACTION_UP'}";
                break;
            default:
                return false;
        }
        socket.emit("new message", drawInfo);
        invalidate();
        return true;
    }

    public void setUserName(String s){
        this.username = s;
    }

    public void setColor(String newColor){
        invalidate();
        paintColor = Color.parseColor(newColor);
        mPaint.setColor(paintColor);
    }

    public void sendDanmaku(String msg){
        messageInfo = "{'action':'DANMAKU','danmaku':'" + msg + "'}" ;
        socket.emit("new message", messageInfo);
    }

    public void sendChangedColor(String cc){
        String changeColor= "{'action':'CHANGE_COLOR','color':'" + cc + "'}";
        socket.emit("new message", changeColor);
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener(){
        @Override
        public void call(final Object... args) {
            JSONObject data= (JSONObject)args[0];
            try{
                String message =  data.getString("message");
                Message msg = handler.obtainMessage();
                msg.obj = message;
                handler.sendMessage(msg);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    };
}
