package com.example.ex_06_painting;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Line {

    // 점. 고정되어있으므로 그대로 써야한다
    // GPU 를 이용하여 고속 계산하여 화면 처리하기 위한 코드
    String vertexShaderString =
            "attribute vec3 aPosition;" // 3개의 값
                    + "attribute vec4 aColor;" // 4개의 값
                    + "uniform mat4 uMVPMatrix;" //(4X4 형태의 상수로 지정)
                    + "varying vec4 vColor;" // 4개의 값

                    + "void main () {"

                    + "vColor = aColor;"
                    + "gl_Position = uMVPMatrix * vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);"
                    // gl_Position : OpenGL에 있는 변수 이용 > 계산식 uMVPMatrix * vPosition

                    + "}";

    // 화면에 어떻게 그려지는지
    String fragmentShaderString =
            // 정밀도 중간
            "precision mediump float;"
                    + "varying vec4 vColor;" // 4 개 (점들) 컬러를 받겠다
                    + "void main() {"
                    + "gl_FragColor = vColor;"
                    +"}";


    float [] mModelMatrix = new float[16];
    float [] mViewMatrix = new float[16];
    float [] mProjMatrix = new float[16];

    float [] mColor = new float[]{1.0f,1.0f,1.0f,1.0f};

    //현재 점 번호
    int mNumPoints = 0;

    // 최대 점 갯수 (점의 배열 좌표 갯수와 동일), 편의성
    int maxPoints = 1000;

    // 1000개의 점 * xyz ( 너무 많이 그리면 라인이 감당이 안된다)
    float [] mPoint = new float[maxPoints * 3];


    FloatBuffer mVertices;
    FloatBuffer mColors;
    ShortBuffer mIndices;
    int mProgram;

    boolean isInited = false;

    int [] mVbo;

    // 새로운 라인 만들기
    public  Line(){
        
    }



    // 그리기 직전에 좌표 수정
    void update(){

        // buffer로 변환

        //점
        mVertices = ByteBuffer.allocateDirect(mPoint.length * 4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mPoint);
        mVertices.position(0);

        // 바인드
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVbo[0]);

        // 버퍼 데이터를 읽어온다
        // 사용할 것, 데이터 크기 (Float.BYTES) = 4 , 버퍼, int 2 (모름)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mNumPoints * 3 * Float.BYTES, null, GLES20.GL_DYNAMIC_DRAW);

        // 사용이 끝난 후 원위치로
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);


        //색
        mColors = ByteBuffer.allocateDirect(mColor.length * 4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(mColor);
        mColors.position(0);


        // 순서
        // short 는 * 2
        mIndices = ByteBuffer.allocateDirect(indices.length * 2).
                order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(indices);
        mIndices.position(0);


    }

    void updatePoint(float x, float y, float z){

        // 현재 점 번호에 좌표 받는다.
        // mNumPoint * 3 + 0번지
        mPoint[mNumPoints * 3 + 0] = x;
        mPoint[mNumPoints * 3 + 1] = y;
        mPoint[mNumPoints * 3 + 2] = z;

        mNumPoints++; // 현재 점 번호 증가
    }


    // 초기화
    void init(){

        mVbo = new int[1];
        
        // 1개 , mVbo 의 0번지부터
        GLES20.glGenBuffers(1, mVbo, 0);
        
        // 바인드
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVbo[0]);

        // 버퍼 데이터를 읽어온다
        // 사용할 것, 데이터 크기 (Float.BYTES) = 4 , 버퍼, int 2 (모름)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, maxPoints * 3 * Float.BYTES, null, GLES20.GL_DYNAMIC_DRAW);

        // 사용이 끝난 후 원위치로
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        
        
        // shading 입체감
        // 점위치 계산식
        // 기존에 GPU로 연산하던 코드를 가져다가 사용
        // 점 쉐이더 생성
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader,vertexShaderString);

        // 컴파일
        GLES20.glCompileShader(vShader);

        // 텍스처
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader,fragmentShaderString);

        // 컴파일
        GLES20.glCompileShader(fShader);


        // mProgram = vertexShader + fragmentShader
        mProgram = GLES20.glCreateProgram();
        // 점위치 계산식 합치기
        GLES20.glAttachShader(mProgram,vShader);
        // 색상 계산식 합치기
        GLES20.glAttachShader(mProgram,fShader);

        GLES20.glLinkProgram(mProgram); // 도형 렌더링 계산식 정보를 넣는다.

        isInited = true;
    }


    int mPositionHandle, mColorHandle, mMVPMatrixHandle;


    // 도형 그리기 --> MyGLRenderer.onDrawFrame() 에서 호출하여 그리기
    void draw(){

        //계산된 렌더링 정보 사용한다.
        GLES20.glUseProgram(mProgram);


        // 핸들러

        // 점, 색 계산방식
        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int color = GLES20.glGetAttribLocation(mProgram, "aColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        float [] mvpMatrix = new float[16];
        float [] mvMatrix = new float[16];


        // 합친다
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0 , mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0 , mvMatrix, 0);

        // mvp 번호에 해당하는 변수에 mvpMatrix 대입
        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix,0);


        // 점, 색 번호에 해당하는 변수에 각각 대입
        // position, 개수, 자료형, 정규화 할것이냐, 스타일 간격(자료형), 좌표
        // 점 float * 3점 (삼각형)
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT,false,4 * 3,mVertices);


        // 색 float * rgba
        GLES20.glVertexAttribPointer(color, 3, GLES20.GL_FLOAT,false,4 * 4,mColors);

        // GPU 활성화
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(color);

        // 선 두께

        GLES20.glLineWidth(50f);

        // 그린다
        //                       배열을 그린다,  0부터,   몇개
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP,0,mNumPoints);

        // GPU 비활성화
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(color);

    }

    // 캡슐화
    void setmModelMatrix(float [] matrix){
        System.arraycopy(matrix,0,mModelMatrix,0,16);
    }

    void updateProjMatrix(float [] projMatrix) {
        System.arraycopy(projMatrix, 0, this.mProjMatrix, 0, 16);
    }

    void updateViewMatrix(float [] viewMatrix) {
        System.arraycopy(viewMatrix, 0, this.mViewMatrix, 0, 16);
    }

}
