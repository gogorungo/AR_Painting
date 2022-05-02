package com.example.ex_06_painting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Session mSession;
    GLSurfaceView mySerView;

    CheckBox chBox;

    MainRenderer mRenderer;
    Config mConfig; // ARCore session 설정 정보를 받을 변수

    // 디스플레이 화면의 X, Y
    float displayX, displayY;

    // 새로운 선인가, 점 추가인가
    boolean mNewPath = false, mPointAdd = false;
    
    // 이전 점을 받을 배열
    float [] lastPoint = {0.0f,0.0f,0.0f};

    float same_dist = 0.001f;

    float [] projMatrix = new float[16];
    float [] viewMatrix = new float[16];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 타이틀바 없애기
        hideStatusBar();
        setContentView(R.layout.activity_main);

        mySerView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        chBox = (CheckBox) findViewById(R.id.checkBox);


        // MainActivity 의 화면 관리 매니져 --> 화면 변화를 감지 : 현재 시스템에서 서비스 지원 (AppCompatActivity)
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        // 화면 변화가 발생되면 MainRenderer의 화면 변환을 실행시킨다.
        if(displayManager != null){
            //화면에 대한 리스너 실행
            displayManager.registerDisplayListener(

                    //익명 클래스로 정의
                    new DisplayManager.DisplayListener() {


                        @Override
                        public void onDisplayAdded(int i) {

                        }

                        @Override
                        public void onDisplayRemoved(int i) {

                        }

                        // 화면이 변경되었다면
                        @Override
                        public void onDisplayChanged(int i) {
                            synchronized (this) {
                                // 화면 갱신 인지 메소드 실행
                                mRenderer.onDisplayChanged();
                            }
                        }
                    } , null);
        }

        MainRenderer.RenderCallBack mr = new MainRenderer.RenderCallBack() {
            // 익명 이너클래스. 생성시에 한번만 사용(정의)가능. 단발적

            // 렌더링 작업
            @Override
            public void preRender() {

                //화면이 회전 되었다면 (필수)
                if(mRenderer.viewprotChanged){
                    // 현재 화면 가져오기
                    Display display = getWindowManager().getDefaultDisplay();

                    mRenderer.updateSession(mSession, display.getRotation());
                }

                // session 객체와 연결해서 화면 그리기 하기
                mSession.setCameraTextureName(mRenderer.getTextureId());

                // 화면 그리기에서 사용할 frame --> session 이 업데이트 되면 새로운 프레임을 받는다
                Frame frame = null;

                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                // 화면을 바꾸기 위한 작업
                mRenderer.transformDisplayGeometry(frame);
                // 위랑 똑같은 기능
//                mRenderer.mCamera.transformDisplayGeometry(frame);



                // 여기서부터가 PointCloud 설정 구간

                // ARCore 에 정의된 클래스
                // 현재 프레임에서 특정있는 점들에 대한 포인트 값 (3차원 좌표값)을 받을 객체
                PointCloud pointCloud = frame.acquirePointCloud();

                // 포인트 값을 적용시키기 위해 mainRenderer -> PointCloud.update() 실행
                mRenderer.mPointCloud.update(pointCloud);

                // 사용이 끝난 포인트 자원해제 (반드시)
                pointCloud.release();

                // 화면 터치시 작업 끝

                // 카메라 frame 에서 받는다
                // mPointCloud 에서 렌더링 할때 카메라의 좌표계산을 받아서 처리
                Camera camera = frame.getCamera();

                camera.getProjectionMatrix(projMatrix,0,0.1f,100.0f);
                camera.getViewMatrix(viewMatrix,0);

//                mRenderer.mPointCloud.updateMatrix(viewMatrix, projMatrix);

                mRenderer.updateProjMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);


                // 선 그리기
                if(chBox.isChecked()){ // 스크린 그리기 상태

                    float [] screenPoint = getScreenPoint(
                            displayX, displayY,
                            mRenderer.width, mRenderer.height,
                            projMatrix, viewMatrix);


                    // 체크가 아닐 때 그린다
                    // 새로운 선 그리기
                    if(mNewPath){
                        // 터치 플래그를 초기화
                        mNewPath = false;

                            // 새로운 라인 그리기
                        mRenderer.addLine(screenPoint[0],screenPoint[1],screenPoint[2]);

                        lastPoint[0] = screenPoint[0];
                        lastPoint[1] = screenPoint[1];
                        lastPoint[2] = screenPoint[2];


                    } else if(mPointAdd){ // 점 추가 라면


                        if(sameChk(screenPoint[0],screenPoint[1],screenPoint[2])){
                            // 점 추가
                            mRenderer.addPoint(screenPoint[0],screenPoint[1],screenPoint[2]);

                            lastPoint[0] = screenPoint[0];
                            lastPoint[1] = screenPoint[1];
                            lastPoint[2] = screenPoint[2];

                        }
                        mPointAdd = false;
                    }

                } else { // 일반 3D 좌표 상태
                    // 체크가 아닐 때 그린다
                    // 새로운 선 그리기
                    if(mNewPath){
                        // 터치 플래그를 초기화
                        mNewPath = false;

                        List<HitResult> arr = frame.hitTest(displayX,displayY);

                        for(HitResult hr : arr){

                            // 축
                            Pose pose = hr.getHitPose();

                            // 새로운 라인 그리기
                            mRenderer.addLine(pose.tx(),pose.ty(),pose.tz());

                            lastPoint[0] = pose.tx();
                            lastPoint[1] = pose.ty();
                            lastPoint[2] = pose.tz();

                            break;
                        }
                    } else if(mPointAdd){ // 점 추가 라면

                        List<HitResult> arr = frame.hitTest(displayX,displayY);

                        for(HitResult hr : arr) {

                            // 축
                            Pose pose = hr.getHitPose();

                            if(sameChk(pose.tx(), pose.ty(), pose.tz())){
                                // 점 추가
                                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());

                                lastPoint[0] = pose.tx();
                                lastPoint[1] = pose.ty();
                                lastPoint[2] = pose.tz();

                                break;
                            }
                        }
                        mPointAdd = false;
                    }
                }
            }
        };

        mRenderer = new MainRenderer(mr);

        // pause 시 관련 데이터가 사라지는 것을 막는다
        mySerView.setPreserveEGLContextOnPause(true);

        // 오픈 EGL 사용시 버전을 2.0 사용
        mySerView.setEGLContextClientVersion(2);

        // 화면을 그리는 Renderer 를 지정한다.
        // 새로 정의한 MainRenderer를 사용한다.
        mySerView.setRenderer(mRenderer);

        // 렌더링 계속 호출
        mySerView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }




    @Override
    protected void onPause() {
        super.onPause();

        mySerView.onPause();
        mSession.pause();
    }

    @Override
    protected void onResume() {
        // 화면을 띄울때마다 실행
        super.onResume();
        cameraPerm();

        try {
            if(mSession == null) {

//                Log.d("session requestInstall ? ",
//                        ArCoreApk.getInstance().requestInstall(this,true)+"");

                // ARcore 가 정상적으로 설치 돼 있는가
                switch (ArCoreApk.getInstance().requestInstall(this,true)){

                    case INSTALLED: // ARcore 정상설치됨
                        //ARcore가 정상설치되어서 session 을 생성 가능한 형태임
                        mSession = new Session(this);
                        Log.d("session 인감","session 생성됨");
                        break;

                    case INSTALL_REQUESTED: // ARcore 설치 필요

                        Log.d("session 인감","ARcore INSTALL_REQUESTED");
                        break;

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 화면 갱신시 세션설정 정보를 받아서 내세션의 설정으로 올린다.
        mConfig = new Config(mSession);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

        mySerView.onResume();

//        mySerView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        Log.d("MainActivity : ", "건드렸다 : " + event.getX() + ","+ event.getY());

        displayX = event.getX();
        displayY = event.getY();

        switch (event.getAction()){

            // 맨 처음 찍기 (선 그리기 시작)
            case MotionEvent.ACTION_DOWN:
                mNewPath = true; // 새로운 선 그린다
                mPointAdd = true; // 점 추가 한다
                break;

            // 선 그리는 중
            case MotionEvent.ACTION_MOVE:
                mPointAdd = true; // 점 추가 한다
                break;

            // 선 그리기 끝
            case MotionEvent.ACTION_UP:
                mPointAdd = false; // 점 추가 완료
                break;

        }


        // 건드렸으면 true

        return true;
    }

    //카메라 퍼미션 요청
    void cameraPerm(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0
            );
        }
    }

    void hideStatusBar(){

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    boolean sameChk(float x, float y, float z){
        float dx = x - lastPoint[0];
        float dy = y - lastPoint[1];
        float dz = z - lastPoint[2];
        boolean res =  Math.sqrt(dx*dx + dy*dy + dz*dz) > same_dist;

        return res;
    }

    public void removeBtnGo(View view){
        mRenderer.removePath();
    }

    //  스크린의 앞쪽에 배치되는 평면
    float [] getScreenPoint(
            float x, float y, // 현재 찍힌 좌표
            float w, float h, // 화면 크기
            float [] projMatrix, float [] viewMatrix){ // 매트릭스들


        float[] position = new float[3];
        float[] direction = new float[3];

        x = x * 2 / w - 1.0f;
        y = (h - y) * 2 / h - 1.0f;

        float[] viewProjMat = new float[16];
        Matrix.multiplyMM(viewProjMat, 0, projMatrix, 0, viewMatrix, 0);

        float[] invertedMat = new float[16];
        Matrix.setIdentityM(invertedMat, 0);
        Matrix.invertM(invertedMat, 0, viewProjMat, 0);

        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];

        Matrix.multiplyMV(nearPlanePoint, 0, invertedMat, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedMat, 0, farScreenPoint, 0);

        position[0] = nearPlanePoint[0] / nearPlanePoint[3];
        position[1] = nearPlanePoint[1] / nearPlanePoint[3];
        position[2] = nearPlanePoint[2] / nearPlanePoint[3];

        direction[0] = farPlanePoint[0] / farPlanePoint[3] - position[0];
        direction[1] = farPlanePoint[1] / farPlanePoint[3] - position[1];
        direction[2] = farPlanePoint[2] / farPlanePoint[3] - position[2];

        //이건 평면을 만드는거 같다
        normalize(direction);

        position[0] += (direction[0] * 0.1f);
        position[1] += (direction[1] * 0.1f);
        position[2] += (direction[2] * 0.1f);

        return position;
    }

    //평면을 만드는 거?
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

}