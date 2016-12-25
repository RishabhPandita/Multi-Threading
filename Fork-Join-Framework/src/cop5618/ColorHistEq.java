package cop5618;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ColorHistEq {

	// Use these labels to instantiate you timers. You will need 8 invocations
	// of now()
	static String[] labels = { "getRGB", "convert to HSB", "create brightness map", "parallel prefix",
			"probability array", "equalize pixels", "setRGB" };
	static int BINNO = 256;

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer times = new Timer(labels);
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();
		// get RGB pixels in an array
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();
		// get HSB from RGB array created above
		float[][] hsbarray = Arrays.stream(sourcePixelArray).mapToObj(ii -> makeHSB(ii)).toArray(float[][]::new);
		times.now();
		double[] brightness = new double[sourcePixelArray.length];
		int bIndex = 0;
		for (int j = 0; j < hsbarray.length; j++) {
			brightness[bIndex++] = (double) hsbarray[j][2];
		}
		ArrayList<Double> items1 = new ArrayList<Double>();
		for (int i = 0; i < brightness.length; i++) {
			items1.add(brightness[i]);
		}
		// Create a histogram of number of pixels falling in a particular
		// brightness value
		Map<Double, Long> collection_cumprob = items1.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		times.now();

		// Define bins that are needed to store brigness value
		Long[] bin = new Long[BINNO];
		for (int i = 0; i < BINNO; i++) {
			bin[i] = 0L;
		}
		// calculate cumulative sums of the histogram that we have created above
		for (Entry<Double, Long> entry : collection_cumprob.entrySet()) {
			// calculate index as brightness * total number of bins
			int index = (int) Math.floor(entry.getKey() * ((double) (BINNO - 1)));
			Long count = bin[index];
			count += entry.getValue();
			bin[index] = count;
		}

		BinaryOperator<Long> opt = (f1, f2) -> (f1 + f2);
		Arrays.parallelPrefix(bin, opt);
		times.now();
		// calculate the probability of brightness
		double[] bin_prob = new double[BINNO];
		for (int i = 0; i < BINNO; i++) {
			bin_prob[i] = ((double) bin[i]) / ((double) bin[BINNO - 1]);
		}
		times.now();
		// recreate the hsbarray's brightness
		for (int j = 0; j < hsbarray.length; j++) {
			int index = (int) Math.floor((hsbarray[j][2]) * ((double) (BINNO - 1)));
			hsbarray[j][2] = (float) bin_prob[index];
		}
		// recreate the pixel values
		double[] spa_double = Arrays.stream(hsbarray).mapToDouble(ii -> makeRGB(ii)).toArray();
		times.now();
		int[] spa = new int[spa_double.length];
		for (int i = 0; i < spa_double.length; i++) {
			spa[i] = (int) spa_double[i];
		}
		// create new image
		newImage.setRGB(0, 0, w, h, spa, 0, w);
		times.now();
		return times;
	}

	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer times = new Timer(labels);
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();
		float[][] hsbarray = Arrays.stream(sourcePixelArray).parallel().mapToObj(ii -> makeHSB(ii))
				.toArray(float[][]::new);
		times.now();
		double[] brightness = new double[sourcePixelArray.length];
		int bIndex = 0;
		for (int j = 0; j < hsbarray.length; j++) {
			brightness[bIndex++] = (double) hsbarray[j][2];
		}
		ArrayList<Double> items1 = new ArrayList<Double>();
		for (int i = 0; i < brightness.length; i++) {
			items1.add(brightness[i]);
		}
		Map<Double, Long> collection_cumprob = items1.stream().parallel()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		times.now();
		Long[] bin = new Long[BINNO];
		for (int i = 0; i < BINNO; i++) {
			bin[i] = 0L;
		}

		for (Entry<Double, Long> entry : collection_cumprob.entrySet()) {
			int index = (int) Math.floor(entry.getKey() * ((double) (BINNO - 1)));
			Long count = bin[index];
			count += entry.getValue();
			bin[index] = count;
		}

		BinaryOperator<Long> opt = (f1, f2) -> (f1 + f2);
		Arrays.parallelPrefix(bin, opt);
		times.now();
		double[] bin_prob = new double[BINNO];
		for (int i = 0; i < BINNO; i++) {
			bin_prob[i] = ((double) bin[i]) / ((double) bin[BINNO - 1]);
		}
		times.now();
		for (int j = 0; j < hsbarray.length; j++) {
			int index = (int) Math.floor((hsbarray[j][2]) * ((double) (BINNO - 1)));
			hsbarray[j][2] = (float) bin_prob[index];
		}

		double[] spa_double = Arrays.stream(hsbarray).parallel().mapToDouble(ii -> makeRGB(ii)).toArray();
		times.now();
		int[] spa = new int[spa_double.length];
		for (int i = 0; i < spa_double.length; i++) {
			spa[i] = (int) spa_double[i];
		}
		newImage.setRGB(0, 0, w, h, spa, 0, w);
		times.now();
		return times;
	}

	/* return an rgb pixel after combining HSB from HSB Array */
	private static int makeRGB(float[] x) {
		int rgb = Color.HSBtoRGB(x[0], x[1], x[2]);
		return rgb;
	}

	/* return an HSB array after taking RED BLUE AND GREEN seperately */
	private static Object makeHSB(int ii) {
		// TODO Auto-generated method stub
		ColorModel colorModel = ColorModel.getRGBdefault();
		float[] hsb = new float[3];
		float returnHSB[] = Color.RGBtoHSB((colorModel.getRed(ii)), (colorModel.getGreen(ii)), (colorModel.getBlue(ii)),
				hsb);
		return hsb;

	}
}
