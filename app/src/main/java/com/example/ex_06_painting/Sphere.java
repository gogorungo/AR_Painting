package com.example.ex_06_painting;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Sphere {




    float [][] sColorArr = {

            {0.0f, 0.0f, 0.0f, 0.0f},

            {1.0f, 1.0f, 1.0f, 1.0f},

            {1.0f, 0.0f, 0.0f, 1.0f},

            {0.0f, 1.0f, 0.0f, 1.0f},

            {0.0f, 0.0f, 1.0f, 1.0f},

    };

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




    // 색깔 (빨간색에 가까운 색)
    float [] mColor = {0.2f, 0.5f, 0.8f, 1.0f };


    FloatBuffer[] sColorsArr = new FloatBuffer[sColorArr.length];


    FloatBuffer mVertices;
    FloatBuffer mColors;
    ShortBuffer mIndices;
    int mProgram;



    // 점 개수 상수화
    final int POINT_COUNT = 20;


//    float [] colors;

    // 버퍼로 만들어서 쪼개 보낸다
    public Sphere() {

        // 구 모양 점 정보
        // 반지름
        float radius = 0.05f;

        // 점의 개수를 분할해서 본다 (3개의 점이 하나의 면을 이룬다(삼각형, 서울랜드의 구 연상))
        // 20 * 20 개의 삼각형으로 이루어진 구
        float[] vertices = new float[POINT_COUNT * POINT_COUNT * 3];

        // 구를 만드는 점의 정보 ---> 수학 개념 필요
        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                float theta = i * (float) Math.PI / (POINT_COUNT - 1);
                float phi = j * 2 * (float) Math.PI / (POINT_COUNT - 1);
                float x = (float) (radius * Math.sin(theta) * Math.cos(phi));
                float y = (float) (radius * Math.cos(theta));
                float z = (float) -(radius * Math.sin(theta) * Math.sin(phi));
                int index = i * POINT_COUNT + j;
                vertices[3 * index] = x;
                vertices[3 * index + 1] = y;
                vertices[3 * index + 2] = z;
            }
        }

//        setColors();

        // 색상 정보 RGBA (면(삼각형) 갯수 * 4(RGBA))
        float[] colors = new float[POINT_COUNT * POINT_COUNT * 4];


        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {

                // 행, 렬로 되어있는 것을 일직선으로 직렬화
                int index = i * POINT_COUNT + j;

                // mColor의 0번지는 빨간색.
                colors[4 * index + 0] = mColor[0];

                // mColor의 1번지는 녹색
                colors[4 * index + 1] = mColor[1];

                // mColor의 2번지는 파란색
                colors[4 * index + 2] = mColor[2];

                // mColor의 3번지는 투명도
                colors[4 * index + 3] = mColor[3];

            }
        }

        //삼각형들을 그리는 점의 순서 정보 (0,1,2, 0,2,3) 했던것
        // 수학적 개념이 필요

        int numIndices = 2 * (POINT_COUNT - 1) * POINT_COUNT;
        short[] indices = new short[numIndices];
        short index = 0;
        for (int i = 0; i < POINT_COUNT - 1; i++) {
            if ((i & 1) == 0) {
                for (int j = 0; j < POINT_COUNT; j++) {
                    indices[index++] = (short) (i * POINT_COUNT + j);
                    indices[index++] = (short) ((i + 1) * POINT_COUNT + j);
                }
            } else {
                for (int j = POINT_COUNT - 1; j >= 0; j--) {
                    indices[index++] = (short) ((i + 1) * POINT_COUNT + j);
                    indices[index++] = (short) (i * POINT_COUNT + j);
                }
            }
        }


        // buffer로 변환

        //점
        mVertices = ByteBuffer.allocateDirect(vertices.length * 4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(vertices);
        mVertices.position(0);


        //색
        mColors = ByteBuffer.allocateDirect(colors.length * 4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(colors);
        mColors.position(0);


        // 순서
        // short 는 * 2
        mIndices = ByteBuffer.allocateDirect(indices.length * 2).
                order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(indices);
        mIndices.position(0);

    }





    // 초기화
    void init(){
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
    }


    int mPositionHandle, mColorHandle, mMVPMatrixHandle;

    // 도형 그리기 --> MyGLRenderer.onDrawFrame() 에서 호출하여 그리기
    void draw(){
//        addNoCnt();
//        setColors();

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

        // 그린다
        //                       삼각형으로 그린다,     순서의 보유량,           순서 자료형,      순서 내용
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP,mIndices.capacity(),GLES20.GL_UNSIGNED_SHORT, mIndices);

        // GPU 비활성화
        GLES20.glDisableVertexAttribArray(position);

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
