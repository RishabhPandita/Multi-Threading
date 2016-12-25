package cop5618;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.BeforeClass;

public class FJBufferedImage extends BufferedImage {

	static int threshhold = 100;
	static AtomicInteger in = new AtomicInteger(0);

	public static class FJBufferedImageFJ extends RecursiveAction {

		int xStart;
		int yStart;
		int w;
		int h;
		int[] rgbArray;
		int offset;
		int scansize;
		static int[] sourcePixelArray;
		int flag;
		StringBuilder sourceSPA = new StringBuilder();
		ArrayList<Integer> splist = new ArrayList<>();

		public int[] getSourcePixelArray() {
			return FJBufferedImageFJ.sourcePixelArray;
		}

		public void setSourcePixelArray(int[] mem) {
			this.sourcePixelArray = mem;
		}

		int index;
		FJBufferedImage fji = null;

		public FJBufferedImageFJ(int xStart2, int yStart2, int w2, int h2, int[] rgbArray2, int offset2, int scansize2,
				FJBufferedImage fjimage, int flag, int getmesize[]) {

			this.xStart = xStart2;
			this.yStart = yStart2;
			this.w = w2;
			this.h = h2;
			this.offset = offset2;
			this.scansize = scansize2;
			this.rgbArray = rgbArray2;
			this.index = 0;
			this.flag = flag;
			this.fji = fjimage;
			this.sourcePixelArray = getmesize;
		}

		@Override
		protected void compute() {
			//compute directly implementation 
			if ((h < threshhold) && flag == 1) {
				offset = (offset + ((yStart) * scansize) + (xStart));
				int[] sourcePixelArray1 = fji.get_set_RGB(xStart, yStart, w, h, rgbArray, offset, scansize, 1);
				for (int i = offset; i < (offset + h * scansize); i++) {
					sourcePixelArray[i] = sourcePixelArray1[i];
				}
				return;
			} else if ((h < threshhold) && flag == 2) {
				offset = (offset + ((yStart) * scansize) + (xStart));
				fji.get_set_RGB(xStart, yStart, w, h, rgbArray, offset, scansize, 2);
				return;
			}

			//if compute directly does not work then divide it 
			int splith = h / 2;
			invokeAll(
					new FJBufferedImageFJ(xStart, yStart, w, splith, rgbArray, offset, scansize, fji, flag,
							sourcePixelArray),
					new FJBufferedImageFJ(xStart, yStart + splith, w, h - splith, rgbArray, offset, scansize, fji, flag,
							sourcePixelArray));
		}
	}

	/** Constructors */
	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}
	//use this function to call bufferedImages getRGB and setRGB , we can also use our own implementation which I have written at last of this file
	public int[] get_set_RGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize, int flag) {

		if (flag == 1)
			return super.getRGB(xStart, yStart, w, h, rgbArray, offset, scansize);
		else {
			super.setRGB(xStart, yStart, w, h, rgbArray, offset, scansize);
		}
		return null;
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
			Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}

	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 * 
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source) {
		Hashtable<String, Object> properties = null;
		String[] propertyNames = source.getPropertyNames();
		if (propertyNames != null) {
			properties = new Hashtable<String, Object>();
			for (String name : propertyNames) {
				properties.put(name, source.getProperty(name));
			}
		}
		return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(),
				properties);
	}

	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		//initial memmory given to rgbarray
		int[] mem = new int[w * h];
		// **** IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER ****
		ForkJoinPool pool = new ForkJoinPool();
		//passing 2 so that my nested class knows it has to call setRGB
		FJBufferedImageFJ fj = new FJBufferedImageFJ(xStart, yStart, w, h, rgbArray, offset, scansize, this, 2, mem);
		pool.invoke(fj);
	}

	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		/**** IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER *****/
		//initial memmory given to rgbarray
		int[] mem = new int[w * h];
		ForkJoinPool pool = new ForkJoinPool();
		// passing 1 so that my nested class knows it has to call getRGB
		FJBufferedImageFJ fj = new FJBufferedImageFJ(xStart, yStart, w, h, rgbArray, offset, scansize, this, 1, mem);
		pool.invoke(fj);
		return fj.getSourcePixelArray();
	}
	
	/*This is an own implementation of getRGB for debugging purposes*/
	public int[] getr(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
		int yoff = offset;
		int off;
		Object data;
		WritableRaster raster = super.getRaster();
		ColorModel colorModel = super.getColorModel();
		int nbands = raster.getNumBands();
		int dataType = raster.getDataBuffer().getDataType();
		switch (dataType) {
		case DataBuffer.TYPE_BYTE:
			data = new byte[nbands];
			break;
		case DataBuffer.TYPE_USHORT:
			data = new short[nbands];
			break;
		case DataBuffer.TYPE_INT:
			data = new int[nbands];
			break;
		case DataBuffer.TYPE_FLOAT:
			data = new float[nbands];
			break;
		case DataBuffer.TYPE_DOUBLE:
			data = new double[nbands];
			break;
		default:
			throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
		}

		if (rgbArray == null) {
			rgbArray = new int[offset + h * scansize];
		}

		for (int y = startY; y < startY + h; y++, yoff += scansize) {
			off = yoff;
			for (int x = startX; x < startX + w; x++) {
				rgbArray[off++] = colorModel.getRGB(raster.getDataElements(x, y, data));
			}
		}

		return rgbArray;
	}
}