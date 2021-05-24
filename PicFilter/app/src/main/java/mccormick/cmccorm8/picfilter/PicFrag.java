package mccormick.cmccorm8.picfilter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.media.ExifInterface;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class PicFrag extends Fragment {

    View view;
    String TAG = "picFrag";
    FloatingActionButton picFab;
    Button processImg, applyFilter;
    ImageView imageView;
    String imgPath;
    Bitmap imgBmp;
    Canvas imgCanvas;
    Paint boundingBox, lmPaint, contourPoints, eyeLine, faceLine, upEyebrow;

    final static int CAPTURE_FILE = 2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(view == null)
        {
            view = inflater.inflate(R.layout.fragment_pic, container, false);
        }
        else {
            ((ViewGroup) container.getParent()).removeView(view);
        }

        viewSetup();
        paintUtils();

        return view;
    }

    /**
     * Method that setups various app utilities, such as the FAB and any buttons
     */
    public void viewSetup()
    {
        //FAB setup
        picFab= view.findViewById(R.id.fab);
        picFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicPath();
            }
        });


        //Process Image Btn setup
        processImg = view.findViewById(R.id.button);
        //prevents app from crashing on btn press by disabling btn until there is a picture to process
        processImg.setEnabled(false);
        processImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processImage();
            }
        });

        //Apply Filter Btn setup
        applyFilter = view.findViewById(R.id.button2);
        applyFilter.setEnabled(false);
        applyFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyFilter();
            }
        });

        imageView = view.findViewById(R.id.imageView1);

    }

    /**
     * Method that sets up any Paint objects for use when drawing to canvas
     */
    public void paintUtils()
    {
        boundingBox = new Paint();
        boundingBox.setColor(Color.RED);
        boundingBox.setStyle(Paint.Style.STROKE);
        boundingBox.setStrokeWidth(10f);
        boundingBox.setAntiAlias(true);

        lmPaint = new Paint();
        lmPaint.setColor(Color.RED);
        lmPaint.setStyle(Paint.Style.FILL);
        lmPaint.setStrokeWidth(5f);
        lmPaint.setAntiAlias(true);

        contourPoints = new Paint();
        contourPoints.setColor(Color.WHITE);
        contourPoints.setStyle(Paint.Style.FILL);
        contourPoints.setStrokeWidth(15f);
        contourPoints.setAntiAlias(true);

        eyeLine = new Paint();
        eyeLine.setColor(Color.GREEN);
        eyeLine.setStrokeWidth(10f);
        eyeLine.setAntiAlias(true);

        faceLine = new Paint();
        faceLine.setColor(Color.BLUE);
        faceLine.setStrokeWidth(10f);
        faceLine.setAntiAlias(true);

        upEyebrow = new Paint();
        upEyebrow.setColor(Color.parseColor("#F57C00"));
        upEyebrow.setStrokeWidth(10f);
        upEyebrow.setAntiAlias(true);

    }

    /**
     * A method used to get the absolute path of a picture that is taken
     * Uses a camera intent to take a picture
     */
    public void getPicPath()
    {
        File fileDirectory = Objects.requireNonNull(getActivity()).getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File mediaFile = new File(fileDirectory.getPath() +File.separator + "IMG_working.jpg");
        Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(this.getContext()), "mccormick.cmccorm8.picfilter.fileprovider",mediaFile);
        imgPath = mediaFile.getAbsolutePath();
        Log.d("File",imgPath);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(intent, CAPTURE_FILE);
    }

    /**
     * Method that processes the image when the Process Image btn is pressed
     */
    public void processImage()
    {

        InputImage image = InputImage.fromBitmap(imgBmp,0);
        // High-accuracy landmark detection and face classification
        FaceDetectorOptions landmarkOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        FaceDetector faceDetector = FaceDetection.getClient(landmarkOptions);

        drawLandMarks(image, faceDetector);


        // Real-time contour detection
        FaceDetectorOptions contourOptions =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        FaceDetector faceDetector1 = FaceDetection.getClient(contourOptions);

        processContour(image, faceDetector1);
    }

    public void applyFilter()
    {

        InputImage image = InputImage.fromBitmap(imgBmp,0);


        // Real-time contour detection
        FaceDetectorOptions contourOptions =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        FaceDetector faceDetector = FaceDetection.getClient(contourOptions);

        Task<List<Face>> result2 = faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if(faces != null)
                        {
                            Log.d("Face Filter: ", "Face found");
                        }
                        for(Face face: faces)
                        {


                            List<PointF> upLeftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP).getPoints();
                            List<PointF> lowerLeftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM).getPoints();
                            //drawfilter(upLeftEyebrow);
                            List<PointF> upRightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP).getPoints();
                            List<PointF> lowerRightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM).getPoints();
                            drawfilter(upLeftEyebrow, lowerLeftEyebrow, upRightEyebrow, lowerRightEyebrow);

                            //drawfilter(upRightEyebrow);
                            imageView.setImageBitmap(imgBmp);
                            imageView.invalidate();

                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Face Filter: ", "No Face Found ");
                    }
                });

    }

    public void drawfilter(List<PointF> upLeft, List<PointF> lowerLeft, List<PointF> upRight, List<PointF> lowerRight)
    {
        float cX, cY;
        float []lUpperX = new float[upLeft.size()];
        float []lUpperY = new float[upLeft.size()];
        /*float []lBottomX = new float[upLeft.size()];
        float []lBottomY = new float[lowerLeft.size()];

        float []rUpperX = new float[upLeft.size()];
        float []rUpperY = new float[upLeft.size()];
        float []rBottomX = new float[upLeft.size()];
        float []rBottomY = new float[lowerLeft.size()];*/
        for(int i = 0; i < upLeft.size(); i++)
        {
            cX = upLeft.get(i).x;
            lUpperX[i] = cX;
            cY = upLeft.get(i).y;
            lUpperY[i] = cY;
            /*cX = lowerLeft.get(i).x;
            lBottomX[i]= cX;
            cY = lowerLeft.get(i).y;
            lBottomY[i] = cY;*/

        }

        /*for(int i = 0; i < upRight.size(); i++)
        {
            cX = upRight.get(i).x;
            rUpperX[i] = cX;
            cY = upRight.get(i).y;
            rUpperY[i] = cY;
            cX = lowerRight.get(i).x;
            rBottomX[i]= cX;
            cY = lowerRight.get(i).y;
            rBottomY[i] = cY;

        }*/
        int pixel = imgBmp.getPixel((int)lUpperX[upRight.size()/2], (int)((lUpperY[upRight.size()/2])-10));
        int redVal = Color.red(pixel);
        int greenVal = Color.green(pixel);
        int blueVal = Color.blue(pixel);
        Paint filterColor = new Paint();
        filterColor.setColor(Color.rgb(redVal,greenVal,blueVal));
        filterColor.setStyle(Paint.Style.FILL);
        filterColor.setStrokeWidth(30f);
        filterColor.setAntiAlias(true);
        for(int i = 0; i < upLeft.size()-1; i++)
        {
            //imgCanvas.drawCircle(upLeft.get(i).x, points.get(j).y, 75, filterColor);
            //imgCanvas.drawRect();
            //imgCanvas.drawLine(lUpperX[i],lUpperY[i],lUpperX[i+1],lUpperY[i+1],filterColor);
            imgCanvas.drawLine(upLeft.get(i).x, upLeft.get(i).y, upLeft.get(i+1).x,upLeft.get(i+1).y,filterColor);
            imgCanvas.drawLine(upRight.get(i).x,upRight.get(i).y,upRight.get(i+1).x, upRight.get(i+1).y,filterColor);
            for(int j = 0; j < upLeft.size(); j++)
            {
                imgCanvas.drawLine(upLeft.get(i).x, upLeft.get(i).y, lowerLeft.get(j).x, lowerLeft.get(j).y,filterColor);
                imgCanvas.drawLine(upRight.get(i).x, upRight.get(i).y, lowerRight.get(j).x, lowerRight.get(j).y,filterColor);
            }

        }

    }

    /**
     * @
     * Method to draw a point at the landmarks on the face
     * @param image-the image that is being processed and altered
     * @param faceDetector-Object of FaceDetector, detects if there is a face to work with
     */
    public void drawLandMarks(InputImage image, FaceDetector faceDetector)
    {

        Task<List<Face>> result = faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        float cx, cy;

                        if(faces != null)
                        {
                            Log.d("Face LandMark: ", "Face found");
                        }
                        for(Face face: faces)
                        {
                            Rect rect = face.getBoundingBox();
                            imgCanvas.drawRect(rect, boundingBox);
                            //FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                            //leftEar.getPosition();
                            for(FaceLandmark landmark : face.getAllLandmarks())
                            {
                                cx = (landmark.getPosition().x);
                                cy = (landmark.getPosition().y);
                                imgCanvas.drawCircle(cx,cy, 10, lmPaint);

                            }
                            imageView.setImageBitmap(imgBmp);
                            imageView.invalidate();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Face LandMark: ", "No Face Found ");
                    }
                });

    }

    /**
     * Method that processes the image for face contours and calls drawContour method
     * @param image-the image that is being processed and altered
     * @param faceDetector1--Object of FaceDetector, detects if there is a face to work with
     */
    public void processContour(InputImage image, FaceDetector faceDetector1)
    {

        Task<List<Face>> result2 = faceDetector1.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {

                        if(faces != null)
                        {
                            Log.d("Face Contour: ", "Face found");
                        }
                        for(Face face: faces)
                        {
                            List<PointF> faceOutline = face.getContour(FaceContour.FACE).getPoints();
                            drawContour(faceOutline, faceLine);
                            List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
                            drawContour(leftEyeContour, eyeLine);
                            List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                            drawContour(rightEyeContour, eyeLine);
                            List<PointF> upLeftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP).getPoints();
                            drawContour(upLeftEyebrow,eyeLine);
                            List<PointF> lowerLeftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM).getPoints();
                            drawContour(lowerLeftEyebrow, faceLine);
                            List<PointF> upRightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP).getPoints();
                            drawContour(upRightEyebrow, upEyebrow);
                            List<PointF> lowerRightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM).getPoints();
                            drawContour(lowerRightEyebrow, boundingBox);
                            List<PointF> noseBridge = face.getContour(FaceContour.NOSE_BRIDGE).getPoints();
                            drawContour(noseBridge, upEyebrow);
                            List<PointF> noseBottom = face.getContour(FaceContour.NOSE_BOTTOM).getPoints();
                            drawContour(noseBottom, faceLine);
                            List<PointF> upLipTop = face.getContour(FaceContour.UPPER_LIP_TOP).getPoints();
                            drawContour(upLipTop, upEyebrow);
                            List<PointF> upLipBottom = face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
                            drawContour(upLipBottom, faceLine);
                            List<PointF> bottomLipTop = face.getContour(FaceContour.LOWER_LIP_TOP).getPoints();
                            drawContour(bottomLipTop, boundingBox);
                            List<PointF> bottomLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM).getPoints();
                            drawContour(bottomLipBottom, eyeLine);
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Face Contour: ", "No Face Found ");
                    }
                });
    }

    /**
     * Method that draws the contour points and lines between the points
     * @param points- A List of PointF objects, stores the contour points
     * @param paint-A Paint object, used for drawing
     */
    public void drawContour(List<PointF> points, Paint paint)
    {
        float cX, cY;
        float []lineX = new float[points.size()];
        float []lineY = new float[points.size()];


        for(int i = 0; i < points.size(); i++)
        {
            cX = points.get(i).x;
            lineX[i] = cX;

            cY = points.get(i).y;
            lineY[i] = cY;

        }
        for(int j = 0; j <points.size()-1; j++)
        {
            imgCanvas.drawLine(lineX[j], lineY[j], lineX[j+1], lineY[j+1], paint);

        }
        for(int k = 0; k < points.size(); k++)
        {
            imgCanvas.drawPoint(lineX[k], lineY[k], contourPoints);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
       super.onActivityResult(requestCode,resultCode,data);
       imgBmp = loadAndRotateImage(imgPath);
       if(imgBmp != null)
       {
           imgCanvas = new Canvas(imgBmp);
           imageView.setImageBitmap(imgBmp);
           processImg.setEnabled(true);
           applyFilter.setEnabled(true);
           Log.d("Activity Result: ", "Img should be loaded");
       }
       else {
           Log.d("Activity Result: " , "Img failed to load");
       }
    }

    /**
     * method that loads and rotates the saved image
     * @param path-absolute path to the image file
     * @return the image in the appropriate orientation
     */
    public Bitmap loadAndRotateImage(String path)
    {
        int rotate = 0;
        ExifInterface exif;

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);

    }
}