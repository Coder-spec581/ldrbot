package dev.rubasace.linkedin.games_tracker.image;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
class ImageTextExtractor {

    private static final int PADDING = 40;
    private final Tesseract tesseract;

    ImageTextExtractor(final TesseractProperties tesseractProperties) {
        System.setProperty("jna.library.path", tesseractProperties.getLibPath());

        this.tesseract = new Tesseract();
        tesseract.setDatapath(tesseractProperties.getDataPath());
        tesseract.setLanguage("eng+spa");
        tesseract.setPageSegMode(11);
        tesseract.setVariable("user_defined_dpi", "300");
    }

    String extractText(final File inputImage) {
        try {
            File preprocessedImage = preprocessForOCR(inputImage);
            return tesseract.doOCR(preprocessedImage);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private File preprocessForOCR(final File inputImage) {
        try (Mat original = opencv_imgcodecs.imread(inputImage.getAbsolutePath())) {
            // Step 1: Grayscale
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(original, gray, opencv_imgproc.COLOR_BGR2GRAY);

            // Step 2: Threshold
            Mat binary = new Mat();
            opencv_imgproc.threshold(gray, binary, 0, 255, opencv_imgproc.THRESH_BINARY + opencv_imgproc.THRESH_OTSU);

            //            // Step 3: Invert colors
            //            opencv_core.bitwise_not(binary, binary);

            // Step 4: Morphology
            Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(2, 2));
            opencv_imgproc.morphologyEx(binary, binary, opencv_imgproc.MORPH_CLOSE, kernel);

            //            // Step 5: Add padding
            //            opencv_core.copyMakeBorder(binary, binary, PADDING, PADDING, PADDING, PADDING, opencv_core.BORDER_CONSTANT, Scalar.BLACK);

            // Step 6: Upscale
            //            Mat upscaled = new Mat();
            //            opencv_imgproc.resize(binary, upscaled, new Size(binary.cols() * 2, binary.rows() * 2));

            //            Mat sharpened = new Mat();
            //            Mat sharpKernel = new Mat(3, 3, opencv_core.CV_32F, new FloatPointer(
            //                    0, -1, 0,
            //                    -1, 5, -1,
            //                    0, -1, 0
            //            ));
            //            opencv_imgproc.filter2D(upscaled, sharpened, upscaled.depth(), sharpKernel);

            File temp = File.createTempFile("ocr-preprocessed", ".png");
            opencv_imgcodecs.imwrite(temp.getAbsolutePath(), binary);
            return temp;
        } catch (Exception e) {
            throw new RuntimeException("OCR failed", e);
        }
    }

}
