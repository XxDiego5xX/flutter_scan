package com.chavesgu.scan;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.MultiFormatReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class QRCodeDecoder {

    private static final String TAG = "QRCodeDecoder";

    private static byte[] yuvs;

    public static int MAX_PICTURE_PIXEL = 800;

    // Mantengo el nombre original para evitar romper otras clases
    public static final List<BarcodeFormat> allFormats = new ArrayList<BarcodeFormat>() {{
        add(BarcodeFormat.QR_CODE);
        add(BarcodeFormat.CODE_128);
        add(BarcodeFormat.EAN_13);
        add(BarcodeFormat.EAN_8);
    }};

    public static final Map<DecodeHintType, Object> HINTS =
            new EnumMap<DecodeHintType, Object>(DecodeHintType.class) {{
                put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                put(DecodeHintType.POSSIBLE_FORMATS, allFormats);
                put(DecodeHintType.CHARACTER_SET, "utf-8");
            }};

    private static final MultiFormatReader reader = new MultiFormatReader();

    public static String syncDecodeQRCode(Bitmap bitmap) {

        if (bitmap == null) return null;

        bitmap = compressBitmap(bitmap);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        byte[] data = getYUV420sp(width, height, bitmap);

        Result result = decodeImage(data, width, height);

        return result != null ? result.getText() : null;
    }

    private static Bitmap compressBitmap(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = Math.min(
                (float) MAX_PICTURE_PIXEL / width,
                (float) MAX_PICTURE_PIXEL / height
        );

        if (scale >= 1) return bitmap;

        return Bitmap.createScaledBitmap(
                bitmap,
                (int) (width * scale),
                (int) (height * scale),
                true
        );
    }

    private static Result decodeImage(byte[] data, int width, int height) {

        reader.setHints(HINTS);

        try {
            PlanarYUVLuminanceSource source =
                    new PlanarYUVLuminanceSource(
                            data,
                            width,
                            height,
                            0,
                            0,
                            width,
                            height,
                            false
                    );

            BinaryBitmap bitmap =
                    new BinaryBitmap(new GlobalHistogramBinarizer(source));

            Result result = reader.decode(bitmap);
            reader.reset();
            return result;

        } catch (NotFoundException e) {

            try {
                PlanarYUVLuminanceSource source =
                        new PlanarYUVLuminanceSource(
                                data,
                                width,
                                height,
                                0,
                                0,
                                width,
                                height,
                                false
                        );

                BinaryBitmap bitmap =
                        new BinaryBitmap(new HybridBinarizer(source));

                Result result = reader.decode(bitmap);
                reader.reset();
                return result;

            } catch (Exception ex) {
                Log.e(TAG, "Decoding failed (Hybrid)", ex);
            }

        } catch (Exception e) {
            Log.e(TAG, "General decoding error", e);
        }

        reader.reset();
        return null;
    }

    private static byte[] getYUV420sp(int width, int height, Bitmap bitmap) {

        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);

        int frameSize = width * height;
        int requiredSize = frameSize * 3 / 2;

        if (yuvs == null || yuvs.length < requiredSize) {
            yuvs = new byte[requiredSize];
        } else {
            Arrays.fill(yuvs, (byte) 0);
        }

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;
        int index = 0;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff);
                index++;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuvs[yIndex++] = (byte) Math.max(0, Math.min(Y, 255));

                if (j % 2 == 0 && i % 2 == 0) {
                    yuvs[uvIndex++] = (byte) Math.max(0, Math.min(V, 255));
                    yuvs[uvIndex++] = (byte) Math.max(0, Math.min(U, 255));
                }
            }
        }

        return yuvs;
    }
}
