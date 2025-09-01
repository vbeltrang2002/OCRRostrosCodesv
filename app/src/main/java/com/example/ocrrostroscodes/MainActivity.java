package com.example.ocrrostroscodes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 111;
    private static final int REQUEST_IMAGE_PICK = 222; // único request para galería
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView imageView;
    private TextView textResult;

    private Bitmap mSelectedImage;
    private Uri imageUri; // por si lo necesitas en otro flujo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView   = findViewById(R.id.imageView);
        textResult  = findViewById(R.id.textResult);

        Button btCamera     = findViewById(R.id.btCamera);
        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btText       = findViewById(R.id.btText);
        Button btFace       = findViewById(R.id.btFace);
        Button btQR         = findViewById(R.id.btQR);

        // Cámara
        btCamera.setOnClickListener(v -> openCamera());

        // Único botón para seleccionar imagen (no escanea automáticamente)
        btnPickImage.setOnClickListener(v -> pickImage());

        // OCR
        btText.setOnClickListener(v -> {
            if (mSelectedImage != null) OCRfx();
            else Toast.makeText(this, "Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
        });

        // Rostros
        btFace.setOnClickListener(v -> {
            if (mSelectedImage != null) detectarRostros(mSelectedImage);
            else Toast.makeText(this, "Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
        });

        // QR/Barras: escanea SOLO la imagen actualmente mostrada (no abre galería)
        btQR.setOnClickListener(v -> {
            if (mSelectedImage == null) {
                Toast.makeText(this, "Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
                return;
            }
            InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
            scanBarcode(image);
        });

        // Permiso de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        try {
            if (requestCode == REQUEST_CAMERA) {
                mSelectedImage = (Bitmap) (data.getExtras() != null ? data.getExtras().get("data") : null);
                if (mSelectedImage != null) {
                    imageView.setImageBitmap(mSelectedImage);
                    textResult.setText("Imagen cargada desde cámara");
                } else {
                    textResult.setText("No se pudo obtener la imagen de la cámara.");
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                imageUri = data.getData();
                if (imageUri != null) {
                    imageView.setImageURI(imageUri);
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    textResult.setText("Imagen cargada desde galería");
                    // IMPORTANTE: NO escaneamos automáticamente aquí.
                } else {
                    textResult.setText("No se seleccionó ninguna imagen.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            textResult.setText("Error al procesar la imagen.");
        }
    }

    // ======= ML Kit: Códigos de barras =======
    private void scanBarcode(InputImage image) {
        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) {
                        textResult.setText("No se detectaron códigos.");
                        return;
                    }
                    StringBuilder result = new StringBuilder();
                    for (Barcode barcode : barcodes) {
                        result.append("Tipo: ").append(formatToName(barcode.getFormat())).append("\n");
                        result.append("Valor: ").append(barcode.getRawValue()).append("\n\n");
                    }
                    textResult.setText(result.toString());
                })
                .addOnFailureListener(e ->
                        textResult.setText("Error al escanear: " + e.getMessage()));
    }

    private String formatToName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "QR_CODE";
            case Barcode.FORMAT_CODE_128: return "CODE_128";
            case Barcode.FORMAT_CODE_39: return "CODE_39";
            case Barcode.FORMAT_CODE_93: return "CODE_93";
            case Barcode.FORMAT_CODABAR: return "CODABAR";
            case Barcode.FORMAT_DATA_MATRIX: return "DATA_MATRIX";
            case Barcode.FORMAT_EAN_13: return "EAN_13";
            case Barcode.FORMAT_EAN_8: return "EAN_8";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_UPC_A: return "UPC_A";
            case Barcode.FORMAT_UPC_E: return "UPC_E";
            case Barcode.FORMAT_AZTEC: return "AZTEC";
            default: return String.valueOf(format);
        }
    }

    // ======= ML Kit: Rostros =======
    private void detectarRostros(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        textResult.setText("No hay rostros");
                    } else {
                        textResult.setText("Hay " + faces.size() + " rostro(s)");

                        Drawable dr = imageView.getDrawable();
                        if (!(dr instanceof BitmapDrawable)) {
                            textResult.setText("No se pudo editar la imagen mostrada.");
                            return;
                        }
                        Bitmap src = ((BitmapDrawable) dr).getBitmap();
                        Bitmap mutableBitmap = src.copy(Bitmap.Config.ARGB_8888, true);

                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(5);
                        paint.setStyle(Paint.Style.STROKE);

                        for (Face face : faces) {
                            canvas.drawRect(face.getBoundingBox(), paint);
                        }
                        imageView.setImageBitmap(mutableBitmap);
                    }
                })
                .addOnFailureListener(e -> textResult.setText("Error al procesar la imagen"));
    }

    // ======= ML Kit: OCR =======
    private void OCRfx() {
        if (mSelectedImage == null) {
            textResult.setText("No se ha seleccionado ninguna imagen.");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::onSuccessOCR)
                .addOnFailureListener(this::onFailure);
    }

    private void onSuccessOCR(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        StringBuilder resultados = new StringBuilder();
        if (blocks.isEmpty()) {
            resultados.append("No se encontró texto.");
        } else {
            for (Text.TextBlock block : blocks) {
                for (Text.Line line : block.getLines()) {
                    for (Text.Element element : line.getElements()) {
                        resultados.append(element.getText()).append(" ");
                    }
                }
            }
        }
        textResult.setText(resultados.toString());
    }

    private void onFailure(Exception e) {
        textResult.setText("Error al procesar la imagen.");
    }

    // ======= Permisos =======
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
